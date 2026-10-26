package com.nodatanolife.bifusion_be.domain.expert.service;

import com.nodatanolife.bifusion_be.domain.dataset.entity.Dataset;
import com.nodatanolife.bifusion_be.domain.dataset.entity.DatasetType;
import com.nodatanolife.bifusion_be.domain.dataset.repository.DatasetRepository;
import com.nodatanolife.bifusion_be.domain.expert.dto.request.InspectionCreateRequest;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionDecisionResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionDetailResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionDraftResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionImageCommentResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.MyInspectionRequestDetailResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.MyInspectionRequestResponse;
import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertApplicationStatus;
import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertTask;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionProgressStatus;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequest;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequestStatus;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;
import com.nodatanolife.bifusion_be.domain.expert.repository.ExpertApplicationRepository;
import com.nodatanolife.bifusion_be.domain.expert.repository.ExpertTaskRepository;
import com.nodatanolife.bifusion_be.domain.expert.repository.InspectionRequestRepository;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationConfig;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationSampleImage;
import com.nodatanolife.bifusion_be.domain.job.repository.AugmentationConfigRepository;
import com.nodatanolife.bifusion_be.domain.job.repository.AugmentationSampleImageRepository;
import com.nodatanolife.bifusion_be.domain.expert.service.InspectionTargetDisplayResolver.TargetDisplays;
import com.nodatanolife.bifusion_be.domain.file.entity.File;
import com.nodatanolife.bifusion_be.domain.file.repository.FileRepository;
import com.nodatanolife.bifusion_be.domain.notification.entity.NotificationType;
import com.nodatanolife.bifusion_be.domain.notification.entity.TargetType;
import com.nodatanolife.bifusion_be.domain.notification.service.NotificationService;
import com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe;
import com.nodatanolife.bifusion_be.domain.recipe.repository.RecipeForkHistoryRepository;
import com.nodatanolife.bifusion_be.domain.recipe.repository.RecipeRepository;
import com.nodatanolife.bifusion_be.domain.user.entity.RewardHistory;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
import com.nodatanolife.bifusion_be.domain.user.service.PointService;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class InspectionServiceImpl implements InspectionService {

    private static final double PLATFORM_FEE_EXCLUDED_RATE = 0.9;

    private final InspectionRequestRepository inspectionRequestRepository;
    private final AugmentationConfigRepository augmentationConfigRepository;
    private final AugmentationSampleImageRepository augmentationSampleImageRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeForkHistoryRepository recipeForkHistoryRepository;
    private final DatasetRepository datasetRepository;
    private final UserRepository userRepository;
    private final ExpertApplicationRepository expertApplicationRepository;
    private final ExpertTaskRepository expertTaskRepository;
    private final PointService pointService;
    private final NotificationService notificationService;
    private final InspectionRewardPolicy inspectionRewardPolicy;
    private final FileRepository fileRepository;
    private final InspectionTargetDisplayResolver inspectionTargetDisplayResolver;

    /**
     * 전문가 검수 신청
     * - AUGMENTED_DATA(AugmentationConfig), RECIPE(Recipe), UPLOADED_DATA(업로드 Dataset)를 지원한다.
     * - 대상의 존재 여부와 본인 소유 여부를 먼저 검증한다 (validateTargetOwnership).
     * - 같은 사용자 + 같은 대상 조합의 신청이 이미 PENDING/APPROVED로 존재하면 409로 막는다.
     * - REJECTED로 종료된 기존 신청이 있으면, (user, target_type, target_id) DB unique 제약 때문에
     *   새 row를 insert할 수 없어 그 row를 재사용(InspectionRequest.reapply)해 PENDING으로 되돌린다.
     *   이전 거절 이력은 보존하지 않는다.
     * - existsBy 체크만으로는 동시 요청을 완전히 막을 수 없어, 신규 생성 시의 DB unique constraint 위반도
     *   같은 409 예외로 변환해 이중으로 방어한다.
     * - REJECTED·CANCELLED로 끝난 신청은 재신청할 수 있다. 기존 row는 잠가서 읽어, 동시 재신청으로 task가 두 개 배정되지 않게 한다.
     * - 배정할 전문가가 없으면 409로 막는다(포인트 차감 전). 신청이 아무에게도 배정되지 않은 채 남지 않게 한다.
     * - 보유 포인트가 리워드 포인트보다 부족하면 400으로 막는다.
     * - 포인트 차감 → 검증 신청 생성 → 전문가 배정(ExpertTask 생성)을 한 트랜잭션(클래스 레벨 @Transactional)으로 처리한다.
     * - 파라미터로 받은 user(인증 필터에서 조회한 엔티티)는 이 트랜잭션 밖에서 로드된 상태라 그대로 변경 메서드를
     *   호출해도 영속화가 보장되지 않는다. userId로 다시 조회한 관리 대상 엔티티에 포인트를 차감한다.
     */
    @Override
    public InspectionResponse createInspection(User user, InspectionCreateRequest request) {
        InspectionTargetType targetType = request.getTargetType();
        Long targetId = request.getTargetId();
        Integer rewardPoints = request.getRewardPoints();

        InspectionRewardPolicy.RewardRange rewardRange = validateTargetOwnership(user, targetType, targetId);
        inspectionRewardPolicy.validate(rewardPoints, rewardRange);

        Optional<InspectionRequest> existing = inspectionRequestRepository
                .findForUpdate(user, targetType, targetId);

        if (existing.isPresent() && !isReapplicable(existing.get().getStatus())) {
            throw new BusinessException(ErrorCode.INSPECTION_ALREADY_EXISTS);
        }

        User expert = pickExpert(user.getUserId());

        User managedUser = userRepository.getReferenceById(user.getUserId());

        InspectionRequest saved;
        if (existing.isPresent()) {
            InspectionRequest reused = existing.get();
            reused.reapply(request.getReason(), rewardPoints);
            saved = inspectionRequestRepository.save(reused);
        } else {
            InspectionRequest inspectionRequest = InspectionRequest.create(managedUser, targetType, targetId, request.getReason(), rewardPoints);
            try {
                saved = inspectionRequestRepository.saveAndFlush(inspectionRequest);
            } catch (DataIntegrityViolationException e) {
                // 동시 요청으로 existsBy 체크를 통과한 뒤 unique constraint에 걸린 경우
                throw new BusinessException(ErrorCode.INSPECTION_ALREADY_EXISTS);
            }
        }

        // 포인트가 부족하면 400으로 막히고, 위에서 저장한 신청도 함께 롤백된다
        pointService.spend(user.getUserId(), rewardPoints, RewardHistory.TYPE_INSPECTION_REQUEST,
                RewardHistory.TARGET_INSPECTION_REQUEST, saved.getInspectionRequestId());

        ExpertTask task = expertTaskRepository.save(ExpertTask.assign(expert, saved));
        task.assignReviewCode();
        return InspectionResponse.from(saved);
    }

    /**
     * 신청자 취소. 전문가가 검수를 시작하기 전(task PENDING)에만 가능하며, 차감했던 포인트를 돌려준다.
     * - 본인 신청이 아니거나 없으면 404 (다른 사람 신청의 존재를 드러내지 않는다)
     * - 현재 회차 task를 잠가서 읽어, 전문가의 검수 시작·승인·반려와 동시에 처리되지 않게 한다.
     * - 아직 시작되지 않은 task는 기록할 것이 없어 삭제한다 (전문가 목록에서 사라진다).
     * - 취소 후 같은 대상에 다시 신청할 수 있다 (InspectionRequest.reapply).
     */
    @Override
    public InspectionResponse cancelInspection(User user, Long inspectionRequestId) {
        InspectionRequest inspectionRequest = getOwnedRequest(user, inspectionRequestId);

        Optional<ExpertTask> task = expertTaskRepository.findFirstByInspectionRequestOrderByExpertTaskIdDesc(inspectionRequest);
        if (task.isPresent() && !task.get().isPending()) {
            throw new BusinessException(ErrorCode.INSPECTION_CANNOT_CANCEL);
        }

        cancelAndRefund(inspectionRequest, task);
        return InspectionResponse.from(inspectionRequest);
    }

    /**
     * 검수 대상(레시피·데이터셋) 삭제 직전에 호출한다.
     * - 진행 중인(PENDING) 신청이 없으면 아무것도 하지 않는다.
     * - 전문가가 아직 시작하지 않았으면 신청자 취소와 똑같이 취소·포인트 환불한다.
     * - 전문가가 이미 검수를 시작했으면 삭제를 409(INSPECTION_IN_PROGRESS_TARGET_DELETE)로 막는다.
     */
    @Override
    public void cancelForTargetDeletion(Long userId, InspectionTargetType targetType, Long targetId) {
        User user = userRepository.getReferenceById(userId);
        Optional<InspectionRequest> pending = inspectionRequestRepository.findForUpdate(user, targetType, targetId)
                .filter(found -> found.getStatus() == InspectionRequestStatus.PENDING);
        if (pending.isEmpty()) {
            return;
        }

        Optional<ExpertTask> task = expertTaskRepository.findFirstByInspectionRequestOrderByExpertTaskIdDesc(pending.get());
        if (task.isPresent() && !task.get().isPending()) {
            throw new BusinessException(ErrorCode.INSPECTION_IN_PROGRESS_TARGET_DELETE);
        }

        cancelAndRefund(pending.get(), task);
    }

    @Override
    public boolean cancelUnassigned(Long inspectionRequestId) {
        Optional<InspectionRequest> request = inspectionRequestRepository.findById(inspectionRequestId)
                .filter(found -> found.getStatus() == InspectionRequestStatus.PENDING);
        if (request.isEmpty()
                || expertTaskRepository.findFirstByInspectionRequestOrderByExpertTaskIdDesc(request.get()).isPresent()) {
            return false;
        }
        cancelAndRefund(request.get(), Optional.empty());
        return true;
    }

    /**
     * 신청을 취소하고, 아직 시작되지 않은 task는 기록할 것이 없어 삭제한 뒤 차감했던 포인트를 돌려준다.
     */
    private void cancelAndRefund(InspectionRequest inspectionRequest, Optional<ExpertTask> task) {
        inspectionRequest.cancel();
        task.ifPresent(expertTaskRepository::delete);

        Integer rewardPoints = inspectionRequest.getRewardPoints();
        if (rewardPoints != null) {
            pointService.earn(inspectionRequest.getUser().getUserId(), rewardPoints, RewardHistory.TYPE_INSPECTION_REFUND,
                    RewardHistory.TARGET_INSPECTION_REQUEST, inspectionRequest.getInspectionRequestId());
        }
    }

    private static boolean isReapplicable(InspectionRequestStatus status) {
        return status == InspectionRequestStatus.REJECTED || status == InspectionRequestStatus.CANCELLED;
    }

    /**
     * 자동 배정할 전문가를 고른다. 전문가 인증(APPROVED)을 받고 탈퇴하지 않은 사용자 중 신청자 본인을 빼고,
     * 진행 중인(완료되지 않은) task가 가장 적은 전문가 → 가장 오래 배정받지 않은 전문가(이력 없음 우선) → userId 순으로 고른다.
     * 후보가 없으면 409(INSPECTION_NO_AVAILABLE_EXPERT).
     */
    private User pickExpert(Long requesterId) {
        List<Long> candidateIds = expertApplicationRepository
                .findActiveUserIdsByStatus(ExpertApplicationStatus.APPROVED).stream()
                .filter(id -> !id.equals(requesterId))
                .toList();
        if (candidateIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INSPECTION_NO_AVAILABLE_EXPERT);
        }

        Map<Long, ExpertTaskRepository.ExpertWorkload> workloads = expertTaskRepository.findWorkloads(candidateIds).stream()
                .collect(Collectors.toMap(ExpertTaskRepository.ExpertWorkload::getUserId, Function.identity()));

        Comparator<Long> byActiveCount = Comparator.comparingLong(id ->
                workloads.containsKey(id) ? workloads.get(id).getActiveCount() : 0L);
        Comparator<Long> byLastAssignedAt = Comparator.comparing(
                id -> workloads.containsKey(id) ? workloads.get(id).getLastAssignedAt() : null,
                Comparator.nullsFirst(Comparator.naturalOrder()));
        Long expertId = candidateIds.stream()
                .min(byActiveCount.thenComparing(byLastAssignedAt).thenComparing(Comparator.naturalOrder()))
                .orElseThrow();
        return userRepository.getReferenceById(expertId);
    }

    /**
     * 신청자의 "내가 요청한 검수" 목록
     * - 취소된 신청은 빼고, 대상 유형·진행 상태·대상 이름으로 거른 뒤 신청일 최신순으로 페이지 조회한다.
     * - 담당 전문가는 현재 회차 task 기준, 대상 이름·이미지는 종류별로 한 번씩 일괄 조회한다 (N+1 방지).
     * - 페이징은 컨트롤러에서 전역 PageResponse로 감싼다.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<MyInspectionRequestResponse> getMyRequests(User user, InspectionTargetType targetType,
                                                           InspectionProgressStatus status, String keyword,
                                                           int page, int size) {
        String trimmedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        Page<InspectionRequest> requests = inspectionRequestRepository.findMyRequests(
                user, targetType, trimmedKeyword, status != null ? status.name() : null,
                InspectionRequestStatus.CANCELLED, InspectionRequestStatus.APPROVED, InspectionRequestStatus.REJECTED,
                PageRequest.of(page, size));

        Map<Long, ExpertTask> latestTasks = latestTasksOf(requests.getContent());
        TargetDisplays displays = inspectionTargetDisplayResolver.resolve(requests.getContent());

        return requests.map(request -> MyInspectionRequestResponse.of(request, latestTasks.get(request.getInspectionRequestId()),
                MyInspectionRequestResponse.Target.of(request, displays.get(request))));
    }

    /**
     * 신청자의 "내가 요청한 검수" 상세. 본인 신청이 아니거나 없거나 취소된 신청이면 404 (취소 API와 같은 기준).
     */
    @Override
    @Transactional(readOnly = true)
    public MyInspectionRequestDetailResponse getMyRequestDetail(User user, Long inspectionRequestId) {
        InspectionRequest inspectionRequest = getOwnedRequest(user, inspectionRequestId);
        if (inspectionRequest.getStatus() == InspectionRequestStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.INSPECTION_NOT_FOUND);
        }

        List<InspectionRequest> single = List.of(inspectionRequest);
        MyInspectionRequestResponse summary = MyInspectionRequestResponse.of(inspectionRequest,
                latestTasksOf(single).get(inspectionRequestId),
                MyInspectionRequestResponse.Target.of(inspectionRequest,
                        inspectionTargetDisplayResolver.resolve(single).get(inspectionRequest)));
        return MyInspectionRequestDetailResponse.of(inspectionRequest, summary);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<InspectionProgressStatus, Long> getMyRequestCounts(User user) {
        Map<InspectionProgressStatus, Long> counts = new EnumMap<>(InspectionProgressStatus.class);
        for (InspectionProgressStatus status : InspectionProgressStatus.values()) {
            counts.put(status, 0L);
        }
        inspectionRequestRepository.countMyRequestsByProgress(user, InspectionRequestStatus.CANCELLED)
                .forEach(row -> {
                    InspectionProgressStatus status = InspectionProgressStatus.of(row.getRequestStatus(), row.getTaskStatus());
                    if (status != null) {
                        counts.merge(status, row.getCount(), Long::sum);
                    }
                });
        return counts;
    }

    // 신청 ID → 현재 회차 task. 배정되지 않은 신청은 없다
    private Map<Long, ExpertTask> latestTasksOf(List<InspectionRequest> requests) {
        if (requests.isEmpty()) {
            return Map.of();
        }
        return expertTaskRepository.findLatestByInspectionRequestIn(requests).stream()
                .collect(Collectors.toMap(t -> t.getInspectionRequest().getInspectionRequestId(), Function.identity()));
    }

    // 본인 신청만 찾는다. 다른 사람 신청은 존재를 드러내지 않도록 없는 것과 같이 404
    private InspectionRequest getOwnedRequest(User user, Long inspectionRequestId) {
        return inspectionRequestRepository.findById(inspectionRequestId)
                .filter(found -> found.getUser().getUserId().equals(user.getUserId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INSPECTION_NOT_FOUND));
    }

    /**
     * 검수 중인 job(전문가 검수 요청) 상세 조회
     * inspectionId는 ExpertTask.expertTaskId를 가리킨다 (검수 시작 API와 동일한 리소스 기준).
     * - 전문가 인증(APPROVED)을 받지 않은 사용자는 조회할 수 없다. (403)
     * - 본인에게 배정되지 않았거나 존재하지 않는 task는 startTask와 동일하게 404로 처리한다.
     * - 대상 종류에 따라 증강 파라미터·샘플 이미지 / 레시피 / 데이터셋 정보를 채운다.
     */
    @Override
    @Transactional(readOnly = true)
    public InspectionDetailResponse getInspectionDetail(User expert, Long inspectionId) {
        ExpertTask task = getOwnedTask(expert, inspectionId);
        InspectionRequest inspectionRequest = task.getInspectionRequest();

        return switch (inspectionRequest.getTargetType()) {
            case AUGMENTED_DATA -> {
                AugmentationConfig config = getAugmentationConfig(task);
                List<AugmentationSampleImage> images = augmentationSampleImageRepository
                        .findAllByAugmentationConfigOrderByAugmentationSampleImageIdAsc(config);
                yield InspectionDetailResponse.ofAugmentedData(task, config, images);
            }
            case RECIPE -> InspectionDetailResponse.ofRecipe(task, getRecipe(inspectionRequest));
            case UPLOADED_DATA -> InspectionDetailResponse.ofUploadedData(task, getDataset(inspectionRequest));
        };
    }

    /**
     * 검수 최종 코멘트 임시 저장 (상세 화면 상단 "임시 저장" 버튼)
     * 승인/거절 상태(InspectionRequest.status)는 건드리지 않고 ExpertTask.draftFeedback만 갱신한다.
     * 임시 저장이므로 빈 문자열/null도 그대로 허용한다.
     * 검수가 이미 승인/거절로 확정된 건은 400(INSPECTION_ALREADY_PROCESSED)으로 막는다.
     */
    @Override
    public InspectionDraftResponse saveDraftComment(User expert, Long inspectionId, String expertComment) {
        ExpertTask task = getOwnedTask(expert, inspectionId);
        task.validateNotCompleted();
        task.getInspectionRequest().validateNotProcessed();

        task.saveDraftComment(expertComment);
        expertTaskRepository.saveAndFlush(task);

        return InspectionDraftResponse.from(task);
    }

    /**
     * 이미지별 검수 의견 저장 ("이미지 정밀 검수" 모달의 "코멘트 저장" 버튼)
     * imageId는 해당 검수 건(inspectionId)의 AugmentationConfig에 속한 샘플 이미지여야 한다.
     * 샘플 이미지가 없는 레시피·데이터셋 검수 건은 404(INSPECTION_IMAGE_NOT_FOUND).
     * 검수가 이미 승인/거절로 확정된 건은 400(INSPECTION_ALREADY_PROCESSED)으로 막는다.
     */
    @Override
    public InspectionImageCommentResponse saveImageComment(User expert, Long inspectionId, Long imageId, String comment) {
        ExpertTask task = getOwnedTask(expert, inspectionId);
        if (task.getInspectionRequest().getTargetType() != InspectionTargetType.AUGMENTED_DATA) {
            throw new BusinessException(ErrorCode.INSPECTION_IMAGE_NOT_FOUND);
        }
        task.validateNotCompleted();
        task.getInspectionRequest().validateNotProcessed();
        AugmentationConfig config = getAugmentationConfig(task);

        AugmentationSampleImage image = augmentationSampleImageRepository
                .findByAugmentationSampleImageIdAndAugmentationConfig(imageId, config)
                .orElseThrow(() -> new BusinessException(ErrorCode.INSPECTION_IMAGE_NOT_FOUND));

        image.saveComment(comment);
        augmentationSampleImageRepository.saveAndFlush(image);

        return InspectionImageCommentResponse.from(image);
    }

    /**
     * 검수 최종 승인 (하단 "최종 승인하기" 버튼)
     * PENDING 상태에서만 가능하며, 이미 처리된 건은 400(INSPECTION_ALREADY_PROCESSED)으로 막는다.
     * expertComment는 draft 값과 무관하게 요청 바디로 받은 값만 최종 코멘트로 확정한다.
     * 확정과 동시에 ExpertTask도 COMPLETED로 전환하고, feedback에 최종 코멘트를 옮기고 draftFeedback은 비운다.
     * 승인/거절 여부와 무관하게 검수 자체는 완료된 것이므로 ExpertTask.reward만큼 전문가에게 즉시 포인트를 지급한다.
     * 요청자(대상 소유자)에게 승인 알림을 보낸다.
     */
    @Override
    public InspectionDecisionResponse approveInspection(User expert, Long inspectionId, String expertComment) {
        ExpertTask task = getOwnedTaskForUpdate(expert, inspectionId);
        task.validateNotCompleted();
        InspectionRequest inspectionRequest = task.getInspectionRequest();
        DecisionNotice notice = resolveDecisionNotice(task);

        inspectionRequest.approve(expert, expertComment);
        task.complete(expertComment);
        payExpertReward(expert, task);

        notificationService.create(
                inspectionRequest.getUser(),
                notice.approvedType(),
                "검수 승인 안내",
                notice.subject() + " 검수가 승인되었습니다.",
                notice.targetType(),
                notice.targetId()
        );

        return InspectionDecisionResponse.from(task, inspectionRequest);
    }

    /**
     * 검수 최종 거절 (하단 "거절하기" 버튼)
     * PENDING 상태에서만 가능하며, 이미 처리된 건은 400(INSPECTION_ALREADY_PROCESSED)으로 막는다.
     * expertComment(거절 사유)는 컨트롤러 단 validation(@NotBlank)으로 필수 처리한다.
     * 승인/거절 여부와 무관하게 검수 자체는 완료된 것이므로 ExpertTask.reward만큼 전문가에게 즉시 포인트를 지급한다.
     * 요청자(대상 소유자)에게 거절 알림을 보낸다.
     */
    @Override
    public InspectionDecisionResponse rejectInspection(User expert, Long inspectionId, String expertComment) {
        ExpertTask task = getOwnedTaskForUpdate(expert, inspectionId);
        task.validateNotCompleted();
        InspectionRequest inspectionRequest = task.getInspectionRequest();
        DecisionNotice notice = resolveDecisionNotice(task);

        inspectionRequest.reject(expert, expertComment);
        task.complete(expertComment);
        payExpertReward(expert, task);

        notificationService.create(
                inspectionRequest.getUser(),
                notice.rejectedType(),
                "검수 거절 안내",
                notice.subject() + " 검수가 거절되었습니다. 사유: " + expertComment,
                notice.targetType(),
                notice.targetId()
        );

        return InspectionDecisionResponse.from(task, inspectionRequest);
    }

    /**
     * 승인·거절 알림의 대상 종류별 문구와 이동 대상.
     * 증강 데이터는 Job 화면, 레시피·데이터셋은 각 상세 화면으로 이동한다.
     */
    private record DecisionNotice(String subject, NotificationType approvedType, NotificationType rejectedType,
                                  TargetType targetType, Long targetId) {
    }

    private DecisionNotice resolveDecisionNotice(ExpertTask task) {
        InspectionRequest inspectionRequest = task.getInspectionRequest();
        return switch (inspectionRequest.getTargetType()) {
            case AUGMENTED_DATA -> {
                AugmentationConfig config = getAugmentationConfig(task);
                yield new DecisionNotice(
                        "\"" + config.getJob().getProject().getTitle() + "\" 증강 데이터",
                        NotificationType.AUGMENTATION_INSPECTION_APPROVED,
                        NotificationType.AUGMENTATION_INSPECTION_REJECTED,
                        TargetType.JOB,
                        config.getJob().getJobId());
            }
            case RECIPE -> {
                Recipe recipe = getRecipe(inspectionRequest);
                yield new DecisionNotice(
                        "\"" + recipe.getTitle() + "\" 레시피",
                        NotificationType.RECIPE_APPROVED,
                        NotificationType.RECIPE_REJECTED,
                        TargetType.RECIPE,
                        recipe.getRecipeId());
            }
            case UPLOADED_DATA -> {
                Dataset dataset = getDataset(inspectionRequest);
                yield new DecisionNotice(
                        "\"" + dataset.getTitle() + "\" 데이터셋",
                        NotificationType.DATASET_VERIFIED,
                        NotificationType.DATASET_VERIFY_REJECTED,
                        TargetType.DATASET,
                        dataset.getDatasetId());
            }
        };
    }

    /**
     * ExpertTask.reward에서 플랫폼 수수료 10%를 뗀 나머지(90%)를 전문가에게 지급하고 RewardHistory에 기록한다.
     * reward가 설정되지 않은 task(파이프라인상 아직 배정되지 않은 값 등)는 지급 없이 건너뛴다.
     */
    private void payExpertReward(User expert, ExpertTask task) {
        Integer reward = task.getReward();
        if (reward == null || reward <= 0) {
            return;
        }

        int payout = (int) Math.round(reward * PLATFORM_FEE_EXCLUDED_RATE);
        pointService.earnForExpertTask(expert.getUserId(), task, payout);
    }

    /**
     * inspectionId(ExpertTask.expertTaskId)로 "본인에게 배정된 검수 건"을 조회한다.
     * - 전문가 인증(APPROVED)을 받지 않은 사용자는 403.
     * - 존재하지 않거나 본인에게 배정되지 않은 task는 startTask와 동일하게 404(EXPERT_TASK_NOT_FOUND)로 처리한다.
     */
    private ExpertTask getOwnedTask(User expert, Long inspectionId) {
        return getOwnedTask(expert, inspectionId, false);
    }

    /**
     * 승인·반려용. task row를 잠가 신청자 취소와 동시에 처리되지 않게 한다 (ExpertTaskRepository.findForUpdate).
     */
    private ExpertTask getOwnedTaskForUpdate(User expert, Long inspectionId) {
        return getOwnedTask(expert, inspectionId, true);
    }

    private ExpertTask getOwnedTask(User expert, Long inspectionId, boolean forUpdate) {
        if (!expertApplicationRepository.existsByUserAndStatus(expert, ExpertApplicationStatus.APPROVED)) {
            throw new BusinessException(ErrorCode.EXPERT_NOT_CERTIFIED);
        }

        return (forUpdate
                ? expertTaskRepository.findForUpdate(inspectionId, expert)
                : expertTaskRepository.findByExpertTaskIdAndExpert(inspectionId, expert))
                .orElseThrow(() -> new BusinessException(ErrorCode.EXPERT_TASK_NOT_FOUND));
    }

    private AugmentationConfig getAugmentationConfig(ExpertTask task) {
        return augmentationConfigRepository.findById(task.getInspectionRequest().getTargetId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INSPECTION_TARGET_NOT_FOUND));
    }

    /**
     * 전문가 화면용 조회라 삭제된 레시피·데이터셋도 찾는다. 진행 중인 검수가 있으면 삭제가 막히므로
     * 삭제된 대상은 이미 완료된 검수 건에서만 보인다.
     */
    private Recipe getRecipe(InspectionRequest inspectionRequest) {
        return recipeRepository.findById(inspectionRequest.getTargetId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INSPECTION_TARGET_NOT_FOUND));
    }

    private Dataset getDataset(InspectionRequest inspectionRequest) {
        return datasetRepository.findById(inspectionRequest.getTargetId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INSPECTION_TARGET_NOT_FOUND));
    }

    /**
     * 대상의 존재 여부(404)와 소유자 일치 여부(403)를 검증한다. 검수는 본인이 소유한 콘텐츠에 대해서만 신청할 수 있다.
     * - AUGMENTED_DATA: 소유자 정보가 직접 없어 상위 Job의 소유자로 판단한다. 삭제된 증강 데이터는 404.
     * - RECIPE: 삭제된 레시피는 404, 원본을 그대로 복제한 사본은 남의 레시피 그대로라 400(INSPECTION_FORKED_RECIPE_NOT_ALLOWED).
     *   원본 내용을 불러와 다시 등록한 레시피는 forkedFrom이 있어도 신청할 수 있다.
     * - UPLOADED_DATA: 삭제됐거나 업로드(UPLOADED) 데이터셋이 아니면 404. 증강 데이터셋은 AUGMENTED_DATA로 신청한다.
     *
     * @return 대상의 리워드 허용 범위 (InspectionRewardPolicy)
     */
    private InspectionRewardPolicy.RewardRange validateTargetOwnership(User user, InspectionTargetType targetType, Long targetId) {
        return switch (targetType) {
            case AUGMENTED_DATA -> {
                AugmentationConfig config = augmentationConfigRepository.findById(targetId)
                        .filter(found -> !found.isDeleted())
                        .orElseThrow(() -> new BusinessException(ErrorCode.INSPECTION_TARGET_NOT_FOUND));
                validateOwner(user, config.getJob().getUser());
                yield inspectionRewardPolicy.forSize(augmentedDatasetSize(config));
            }
            case RECIPE -> {
                Recipe recipe = recipeRepository.findByRecipeIdAndDeletedAtIsNull(targetId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INSPECTION_TARGET_NOT_FOUND));
                validateOwner(user, recipe.getUser());
                if (recipeForkHistoryRepository.existsByForkedRecipe(recipe)) {
                    throw new BusinessException(ErrorCode.INSPECTION_FORKED_RECIPE_NOT_ALLOWED);
                }
                yield inspectionRewardPolicy.forRecipe();
            }
            case UPLOADED_DATA -> {
                Dataset dataset = datasetRepository.findById(targetId)
                        .filter(found -> !found.isDeleted() && found.getType() == DatasetType.UPLOADED)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INSPECTION_TARGET_NOT_FOUND));
                validateOwner(user, dataset.getUser());
                yield inspectionRewardPolicy.forDataset(dataset);
            }
        };
    }

    // 증강 결과 데이터셋 파일 크기. 결과 파일이 아직 없으면 null
    private Long augmentedDatasetSize(AugmentationConfig config) {
        if (config.getDatasetKey() == null) {
            return null;
        }
        return fileRepository.findAllByS3KeyIn(List.of(config.getDatasetKey())).stream()
                .map(File::getFileSize)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private static void validateOwner(User user, User owner) {
        if (!owner.getUserId().equals(user.getUserId())) {
            throw new BusinessException(ErrorCode.INSPECTION_FORBIDDEN);
        }
    }
}
