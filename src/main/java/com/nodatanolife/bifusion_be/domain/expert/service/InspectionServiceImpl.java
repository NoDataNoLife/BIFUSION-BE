package com.nodatanolife.bifusion_be.domain.expert.service;

import com.nodatanolife.bifusion_be.domain.dataset.entity.Dataset;
import com.nodatanolife.bifusion_be.domain.dataset.repository.DatasetRepository;
import com.nodatanolife.bifusion_be.domain.expert.dto.request.InspectionCreateRequest;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionDetailResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionDraftResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionImageCommentResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionResponse;
import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertApplicationStatus;
import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertTask;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequest;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;
import com.nodatanolife.bifusion_be.domain.expert.repository.ExpertApplicationRepository;
import com.nodatanolife.bifusion_be.domain.expert.repository.ExpertTaskRepository;
import com.nodatanolife.bifusion_be.domain.expert.repository.InspectionRequestRepository;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationConfig;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationSampleImage;
import com.nodatanolife.bifusion_be.domain.job.repository.AugmentationConfigRepository;
import com.nodatanolife.bifusion_be.domain.job.repository.AugmentationSampleImageRepository;
import com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe;
import com.nodatanolife.bifusion_be.domain.recipe.repository.RecipeRepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InspectionServiceImpl implements InspectionService {

    private final InspectionRequestRepository inspectionRequestRepository;
    private final DatasetRepository datasetRepository;
    private final AugmentationConfigRepository augmentationConfigRepository;
    private final AugmentationSampleImageRepository augmentationSampleImageRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final ExpertApplicationRepository expertApplicationRepository;
    private final ExpertTaskRepository expertTaskRepository;

    /**
     * 전문가 검수 신청
     * - 대상(RECIPE/UPLOADED_DATA/AUGMENTED_DATA)의 존재 여부와 본인 소유 여부를 먼저 검증한다.
     * - 같은 사용자 + 같은 대상 조합의 신청이 이미 있으면 409로 막는다. (existsBy 사전 체크)
     * - existsBy 체크만으로는 동시 요청을 완전히 막을 수 없어, DB unique constraint 위반도
     *   같은 409 예외로 변환해 이중으로 방어한다.
     * - 보유 포인트가 리워드 포인트보다 부족하면 400으로 막는다.
     * - 포인트 차감 → 검증 신청 생성을 한 트랜잭션(클래스 레벨 @Transactional)으로 처리한다.
     *   InspectionRequest는 생성 시 status=PENDING으로 저장되며, 대상(Recipe/Dataset)의 검증 상태는
     *   별도 컬럼으로 중복 저장하지 않고 InspectionResultService가 이 status를 조회 시점에 읽어 파생시킨다.
     * - 실제 검수 처리(점수/코멘트 기록)는 전문가/관리자가 이후 status를 APPROVED/REJECTED로 변경하는 별도 플로우에서 이루어진다.
     * - 파라미터로 받은 user(인증 필터에서 조회한 엔티티)는 이 트랜잭션 밖에서 로드된 상태라 그대로 변경 메서드를
     *   호출해도 영속화가 보장되지 않는다. userId로 다시 조회한 관리 대상 엔티티에 포인트를 차감한다.
     */
    @Override
    public InspectionResponse createInspection(User user, InspectionCreateRequest request) {
        InspectionTargetType targetType = request.getTargetType();
        Long targetId = request.getTargetId();
        Integer rewardPoints = request.getRewardPoints();

        validateTargetOwnership(user, targetType, targetId);

        if (inspectionRequestRepository.existsByUserAndTargetTypeAndTargetId(user, targetType, targetId)) {
            throw new BusinessException(ErrorCode.INSPECTION_ALREADY_EXISTS);
        }

        User managedUser = userRepository.getReferenceById(user.getUserId());
        int currentPoints = managedUser.getPoints() == null ? 0 : managedUser.getPoints();
        if (currentPoints < rewardPoints) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINTS);
        }
        managedUser.deductPoints(rewardPoints);

        InspectionRequest inspectionRequest = InspectionRequest.create(managedUser, targetType, targetId, request.getReason(), rewardPoints);
        try {
            InspectionRequest saved = inspectionRequestRepository.save(inspectionRequest);
            return InspectionResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 existsBy 체크를 통과한 뒤 unique constraint에 걸린 경우
            throw new BusinessException(ErrorCode.INSPECTION_ALREADY_EXISTS);
        }
    }

    /**
     * 검수 중인 job(전문가 검수 요청) 상세 조회
     * inspectionId는 ExpertTask.expertTaskId를 가리킨다 (검수 시작 API와 동일한 리소스 기준).
     * - 전문가 인증(APPROVED)을 받지 않은 사용자는 조회할 수 없다. (403)
     * - 본인에게 배정되지 않았거나 존재하지 않는 task는 startTask와 동일하게 404로 처리한다.
     * - AUGMENTED_DATA 검수만 대상으로 한다. (증강 파라미터/샘플 이미지가 있는 대상)
     */
    @Override
    @Transactional(readOnly = true)
    public InspectionDetailResponse getInspectionDetail(User expert, Long inspectionId) {
        ExpertTask task = getOwnedAugmentedDataTask(expert, inspectionId);
        AugmentationConfig config = getAugmentationConfig(task);

        List<AugmentationSampleImage> images = augmentationSampleImageRepository
                .findAllByAugmentationConfigOrderByAugmentationSampleImageIdAsc(config);

        return InspectionDetailResponse.from(task, config, images);
    }

    /**
     * 검수 최종 코멘트 임시 저장 (상세 화면 상단 "임시 저장" 버튼)
     * 승인/거절 상태(InspectionRequest.status)는 건드리지 않고 ExpertTask.draftFeedback만 갱신한다.
     * 임시 저장이므로 빈 문자열/null도 그대로 허용한다.
     */
    @Override
    public InspectionDraftResponse saveDraftComment(User expert, Long inspectionId, String expertComment) {
        ExpertTask task = getOwnedAugmentedDataTask(expert, inspectionId);

        task.saveDraftComment(expertComment);
        expertTaskRepository.saveAndFlush(task);

        return InspectionDraftResponse.from(task);
    }

    /**
     * 이미지별 검수 의견 저장 ("이미지 정밀 검수" 모달의 "코멘트 저장" 버튼)
     * imageId는 해당 검수 건(inspectionId)의 AugmentationConfig에 속한 샘플 이미지여야 한다.
     */
    @Override
    public InspectionImageCommentResponse saveImageComment(User expert, Long inspectionId, Long imageId, String comment) {
        ExpertTask task = getOwnedAugmentedDataTask(expert, inspectionId);
        AugmentationConfig config = getAugmentationConfig(task);

        AugmentationSampleImage image = augmentationSampleImageRepository
                .findByAugmentationSampleImageIdAndAugmentationConfig(imageId, config)
                .orElseThrow(() -> new BusinessException(ErrorCode.INSPECTION_IMAGE_NOT_FOUND));

        image.saveComment(comment);
        augmentationSampleImageRepository.saveAndFlush(image);

        return InspectionImageCommentResponse.from(image);
    }

    /**
     * inspectionId(ExpertTask.expertTaskId)로 "본인에게 배정된 AUGMENTED_DATA 검수 건"을 조회한다.
     * - 전문가 인증(APPROVED)을 받지 않은 사용자는 403.
     * - 존재하지 않거나 본인에게 배정되지 않은 task, 또는 AUGMENTED_DATA가 아닌 검수 건은
     *   startTask와 동일하게 404(EXPERT_TASK_NOT_FOUND)로 처리한다.
     */
    private ExpertTask getOwnedAugmentedDataTask(User expert, Long inspectionId) {
        if (!expertApplicationRepository.existsByUserAndStatus(expert, ExpertApplicationStatus.APPROVED)) {
            throw new BusinessException(ErrorCode.EXPERT_NOT_CERTIFIED);
        }

        ExpertTask task = expertTaskRepository.findByExpertTaskIdAndExpert(inspectionId, expert)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXPERT_TASK_NOT_FOUND));

        if (task.getInspectionRequest().getTargetType() != InspectionTargetType.AUGMENTED_DATA) {
            throw new BusinessException(ErrorCode.EXPERT_TASK_NOT_FOUND);
        }

        return task;
    }

    private AugmentationConfig getAugmentationConfig(ExpertTask task) {
        return augmentationConfigRepository.findById(task.getInspectionRequest().getTargetId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INSPECTION_TARGET_NOT_FOUND));
    }

    /**
     * targetType별로 대상 엔티티를 조회해 존재 여부(404)와 소유자 일치 여부(403)를 검증한다.
     * 검수는 본인이 소유한 콘텐츠에 대해서만 신청할 수 있다.
     */
    private void validateTargetOwnership(User user, InspectionTargetType targetType, Long targetId) {
        switch (targetType) {
            case RECIPE -> {
                Recipe recipe = recipeRepository.findById(targetId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INSPECTION_TARGET_NOT_FOUND));
                if (!recipe.getUser().getUserId().equals(user.getUserId())) {
                    throw new BusinessException(ErrorCode.INSPECTION_FORBIDDEN);
                }
            }
            case UPLOADED_DATA -> {
                Dataset dataset = datasetRepository.findById(targetId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INSPECTION_TARGET_NOT_FOUND));
                if (!dataset.getUser().getUserId().equals(user.getUserId())) {
                    throw new BusinessException(ErrorCode.INSPECTION_FORBIDDEN);
                }
            }
            case AUGMENTED_DATA -> {
                // 증강 데이터는 소유자 정보가 직접 없어 상위 Job의 소유자로 판단한다.
                AugmentationConfig config = augmentationConfigRepository.findById(targetId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INSPECTION_TARGET_NOT_FOUND));
                if (!config.getJob().getUser().getUserId().equals(user.getUserId())) {
                    throw new BusinessException(ErrorCode.INSPECTION_FORBIDDEN);
                }
            }
        }
    }
}
