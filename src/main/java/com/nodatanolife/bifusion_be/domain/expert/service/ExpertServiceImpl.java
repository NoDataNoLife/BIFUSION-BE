package com.nodatanolife.bifusion_be.domain.expert.service;

import com.nodatanolife.bifusion_be.domain.expert.dto.response.AdminExpertApplicationResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.ExpertApplicationResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.ExpertPerformanceResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.ExpertTaskGroupedResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.ExpertTaskSummaryResponse;
import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertApplication;
import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertApplicationStatus;
import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertTask;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequest;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequestStatus;
import com.nodatanolife.bifusion_be.domain.expert.repository.ExpertApplicationRepository;
import com.nodatanolife.bifusion_be.domain.expert.repository.ExpertTaskRepository;
import com.nodatanolife.bifusion_be.domain.expert.repository.InspectionRequestRepository;
import com.nodatanolife.bifusion_be.domain.expert.service.InspectionTargetDisplayResolver.TargetDisplay;
import com.nodatanolife.bifusion_be.domain.expert.service.InspectionTargetDisplayResolver.TargetDisplays;
import com.nodatanolife.bifusion_be.domain.file.service.S3Service;
import com.nodatanolife.bifusion_be.domain.notification.entity.NotificationType;
import com.nodatanolife.bifusion_be.domain.notification.entity.TargetType;
import com.nodatanolife.bifusion_be.domain.notification.service.NotificationService;
import com.nodatanolife.bifusion_be.domain.user.entity.RewardHistory;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.repository.RewardHistoryRepository;
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpertServiceImpl implements ExpertService {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");
    private static final String S3_PREFIX = "expert-application";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private final ExpertApplicationRepository expertApplicationRepository;
    private final ExpertTaskRepository expertTaskRepository;
    private final InspectionRequestRepository inspectionRequestRepository;
    private final InspectionTargetDisplayResolver inspectionTargetDisplayResolver;
    private final RewardHistoryRepository rewardHistoryRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final NotificationService notificationService;

    /**
     * 회원 탈퇴 시 전문가 신청 내역 삭제
     */
    @Override
    public void deleteApplicationByUser(User user) {
        expertApplicationRepository.deleteByUser(user);
    }

    /**
     * 회원 탈퇴 시 전문가 관련 데이터 익명화
     * ExpertTask, InspectionRequest의 전문가/검토자를 탈퇴 계정으로 변경
     */
    @Override
    public void anonymizeByUser(User user, User deletedUser) {
        expertTaskRepository.findAllByExpert(user)
                .forEach(t -> t.anonymize(deletedUser));
        inspectionRequestRepository.findAllByUser(user)
                .forEach(v -> v.anonymize(deletedUser));
        inspectionRequestRepository.findAllByReviewedBy(user)
                .forEach(v -> v.anonymizeReviewer(deletedUser));
    }

    /**
     * 전문가 인증 신청
     * - APPROVED 상태면 차단 (409)
     * - PENDING 상태가 이미 있으면 첨부 파일을 새 파일로 덮어쓰기 (어드민 큐에 1건만 유지)
     * - 그 외에는 새 PENDING row 생성
     */
    @Override
    public ExpertApplicationResponse apply(Long userId, MultipartFile certificationFile) {
        validateFile(certificationFile);

        User user = userRepository.getReferenceById(userId);

        if (expertApplicationRepository.existsByUserAndStatus(user, ExpertApplicationStatus.APPROVED)) {
            throw new BusinessException(ErrorCode.EXPERT_ALREADY_APPROVED);
        }

        String newFileUrl = s3Service.uploadFile(certificationFile, S3_PREFIX);

        Optional<ExpertApplication> existingPending =
                expertApplicationRepository.findByUserAndStatus(user, ExpertApplicationStatus.PENDING);

        ExpertApplication target;
        if (existingPending.isPresent()) {
            target = existingPending.get();
            String oldUrl = target.getApplicationDocumentUrl();
            target.replaceDocument(newFileUrl);
            s3Service.deleteFileByUrl(oldUrl);
        } else {
            target = expertApplicationRepository.save(
                    ExpertApplication.create(user, newFileUrl)
            );
        }

        return ExpertApplicationResponse.from(target);
    }

    /**
     * 전문가 심사 진행 상태 판정
     * 승인 기록이 하나라도 있으면 APPROVED (검수 요청 알림·전문가 목록의 existsByUserAndStatus(APPROVED)와 같은 기준).
     * 승인 후 재신청은 apply에서 막지만, 관리자가 DB에서 상태를 바꿔 승인 뒤에 다른 기록이 생겨도 판정이 어긋나지 않게 한다.
     * 승인 기록이 없으면 가장 최근 신청의 상태, 신청 내역이 없으면 NONE.
     */
    @Override
    @Transactional(readOnly = true)
    public ExpertApplicationStatus resolveStatus(User user) {
        if (expertApplicationRepository.existsByUserAndStatus(user, ExpertApplicationStatus.APPROVED)) {
            return ExpertApplicationStatus.APPROVED;
        }
        return expertApplicationRepository.findFirstByUserOrderByAppliedAtDesc(user)
                .map(ExpertApplication::getStatus)
                .orElse(ExpertApplicationStatus.NONE);
    }

    /**
     * 전문가 성과 대시보드 조회
     * 전문가 인증(APPROVED)을 받지 않은 사용자는 조회할 수 없다.
     * - totalReward: RewardHistory 합계
     * - taskStats: ExpertTask 상태별 건수 (PENDING/IN_PROGRESS/COMPLETED 항상 포함)
     * - topPercent: APPROVED 전문가 중 검수 완료 건수 기준 순위를 퍼센타일로 환산
     */
    @Override
    @Transactional(readOnly = true)
    public ExpertPerformanceResponse getMyPerformance(User user) {
        if (!expertApplicationRepository.existsByUserAndStatus(user, ExpertApplicationStatus.APPROVED)) {
            throw new BusinessException(ErrorCode.EXPERT_NOT_CERTIFIED);
        }

        // 가입 보너스·포크 수익·차감 내역은 빼고 전문가 활동(검수·Q&A 답변) 보상만 합산한다
        int totalReward = rewardHistoryRepository.sumAmountByUserAndTypeIn(user,
                List.of(RewardHistory.TYPE_EXPERT_TASK_REWARD, RewardHistory.TYPE_QNA_ANSWER_REWARD));

        Map<String, Long> taskStats = new LinkedHashMap<>();
        taskStats.put(STATUS_PENDING, 0L);
        taskStats.put(STATUS_IN_PROGRESS, 0L);
        taskStats.put(STATUS_COMPLETED, 0L);
        expertTaskRepository.countGroupedByStatus(user)
                .forEach(row -> taskStats.put(row.getStatus(), row.getCount()));

        long completedCount = taskStats.getOrDefault(STATUS_COMPLETED, 0L);

        BigDecimal avgSatisfaction = expertTaskRepository.findAverageSatisfaction(user);
        Double feedbackSatisfaction = avgSatisfaction == null
                ? null
                : avgSatisfaction.setScale(1, RoundingMode.HALF_UP).doubleValue();

        Integer topPercent = calculateTopPercent(user);

        return new ExpertPerformanceResponse(
                true, totalReward, topPercent, completedCount, feedbackSatisfaction, taskStats);
    }

    private Integer calculateTopPercent(User user) {
        List<Long> approvedExpertIds = expertApplicationRepository
                .findAllByStatus(ExpertApplicationStatus.APPROVED)
                .stream()
                .map(a -> a.getUser().getUserId())
                .distinct()
                .toList();

        if (approvedExpertIds.isEmpty()) {
            return null;
        }

        Map<Long, Long> completedCountByExpert = expertTaskRepository
                .countGroupedByExpertForStatus(STATUS_COMPLETED)
                .stream()
                .collect(Collectors.toMap(
                        ExpertTaskRepository.ExpertCompletionCount::getUserId,
                        ExpertTaskRepository.ExpertCompletionCount::getCount
                ));

        long myCount = completedCountByExpert.getOrDefault(user.getUserId(), 0L);

        long rankedAbove = approvedExpertIds.stream()
                .filter(id -> completedCountByExpert.getOrDefault(id, 0L) > myCount)
                .count();

        long rank = rankedAbove + 1;
        return (int) Math.ceil(rank * 100.0 / approvedExpertIds.size());
    }

    /**
     * 전문가 검수 요청 목록 조회
     * 전문가 인증(APPROVED)을 받지 않은 사용자는 조회할 수 없다.
     * 증강 데이터·레시피·업로드 데이터셋 검수를 모두 담고, 검수 상태별로 묶어 반환한다.
     * - PENDING(검수 전): 전문가 풀. 아직 아무도 시작하지 않은 신청 (본인 신청 제외, 오래된 순)
     * - IN_PROGRESS·COMPLETED: 본인이 시작한 task (updatedAt 최신순)
     * 대상 이름은 종류별로 한 번씩 일괄 조회한다 (InspectionTargetDisplayResolver).
     */
    @Override
    @Transactional(readOnly = true)
    public ExpertTaskGroupedResponse getMyTasks(User user) {
        if (!expertApplicationRepository.existsByUserAndStatus(user, ExpertApplicationStatus.APPROVED)) {
            throw new BusinessException(ErrorCode.EXPERT_NOT_CERTIFIED);
        }

        List<ExpertTask> tasks = expertTaskRepository.findMyReviewTasks(user);
        List<InspectionRequest> pool = inspectionRequestRepository.findPool(user, InspectionRequestStatus.PENDING);
        TargetDisplays displays = inspectionTargetDisplayResolver.resolve(Stream.concat(
                tasks.stream().map(ExpertTask::getInspectionRequest), pool.stream()).toList());

        Map<String, List<ExpertTaskSummaryResponse>> grouped = tasks.stream()
                .map(t -> {
                    TargetDisplay display = displays.get(t.getInspectionRequest());
                    return ExpertTaskSummaryResponse.of(t,
                            display != null ? display.name() : null,
                            display != null ? display.imageUrl() : null);
                })
                .collect(Collectors.groupingBy(ExpertTaskSummaryResponse::status));
        List<ExpertTaskSummaryResponse> pending = pool.stream()
                .map(request -> {
                    TargetDisplay display = displays.get(request);
                    return ExpertTaskSummaryResponse.ofPending(request,
                            display != null ? display.name() : null,
                            display != null ? display.imageUrl() : null);
                })
                .toList();

        return new ExpertTaskGroupedResponse(
                pending,
                grouped.getOrDefault(STATUS_IN_PROGRESS, List.of()),
                grouped.getOrDefault(STATUS_COMPLETED, List.of())
        );
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.EXPERT_FILE_INVALID);
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.EXPERT_FILE_TOO_LARGE);
        }

        String original = file.getOriginalFilename();
        if (original == null || !original.contains(".")) {
            throw new BusinessException(ErrorCode.EXPERT_FILE_INVALID);
        }
        String ext = original.substring(original.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ErrorCode.EXPERT_FILE_INVALID);
        }
    }

    /**
     * 관리자 심사 목록 조회
     * status가 없으면 전체 상태를 조회한다. 정렬은 호출 측 Pageable을 따른다.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<AdminExpertApplicationResponse> getApplications(ExpertApplicationStatus status, Pageable pageable) {
        Page<ExpertApplication> applications = status == null
                ? expertApplicationRepository.findAllBy(pageable)
                : expertApplicationRepository.findAllByStatus(status, pageable);
        return applications.map(AdminExpertApplicationResponse::from);
    }

    /**
     * 전문가 인증 승인
     * PENDING 신청만 승인할 수 있고, 승인되면 신청자에게 EXPERT_APPROVED 알림을 보낸다.
     */
    @Override
    public void approve(Long adminId, Long applicationId) {
        ExpertApplication application = getApplication(applicationId);
        application.approve(adminId);
        notificationService.create(
                application.getUser(),
                NotificationType.EXPERT_APPROVED,
                "전문가 인증 승인",
                "전문가 인증이 승인되었습니다. 이제 검수 요청을 받을 수 있습니다.",
                TargetType.EXPERT,
                application.getExpertApplicationId()
        );
    }

    /**
     * 전문가 인증 반려
     * PENDING 신청만 반려할 수 있고, 반려 사유를 담아 신청자에게 EXPERT_REJECTED 알림을 보낸다.
     */
    @Override
    public void reject(Long adminId, Long applicationId, String reason) {
        ExpertApplication application = getApplication(applicationId);
        application.reject(adminId, reason);
        notificationService.create(
                application.getUser(),
                NotificationType.EXPERT_REJECTED,
                "전문가 인증 반려",
                "전문가 인증이 반려되었습니다. 사유: " + reason,
                TargetType.EXPERT,
                application.getExpertApplicationId()
        );
    }

    private ExpertApplication getApplication(Long applicationId) {
        return expertApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXPERT_APPLICATION_NOT_FOUND));
    }
}
