package com.nodatanolife.bifusion_be.domain.expert.service;

import com.nodatanolife.bifusion_be.domain.dataset.entity.Dataset;
import com.nodatanolife.bifusion_be.domain.dataset.repository.DatasetRepository;
import com.nodatanolife.bifusion_be.domain.expert.dto.request.InspectionCreateRequest;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionResponse;
import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertVerification;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;
import com.nodatanolife.bifusion_be.domain.expert.repository.ExpertVerificationRepository;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationConfig;
import com.nodatanolife.bifusion_be.domain.job.repository.AugmentationConfigRepository;
import com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe;
import com.nodatanolife.bifusion_be.domain.recipe.repository.RecipeRepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InspectionServiceImpl implements InspectionService {

    private final ExpertVerificationRepository expertVerificationRepository;
    private final DatasetRepository datasetRepository;
    private final AugmentationConfigRepository augmentationConfigRepository;
    private final RecipeRepository recipeRepository;

    /**
     * 전문가 검수 신청
     * - 대상(RECIPE/UPLOADED_DATA/AUGMENTED_DATA)의 존재 여부와 본인 소유 여부를 먼저 검증한다.
     * - 같은 사용자 + 같은 대상 조합의 신청이 이미 있으면 409로 막는다. (existsBy 사전 체크)
     * - existsBy 체크만으로는 동시 요청을 완전히 막을 수 없어, DB unique constraint 위반도
     *   같은 409 예외로 변환해 이중으로 방어한다.
     * - 보유 포인트가 리워드 포인트보다 부족하면 400으로 막는다.
     * - 포인트 차감 → 검증 신청 생성을 한 트랜잭션(클래스 레벨 @Transactional)으로 처리한다.
     *   ExpertVerification은 생성 시 status=PENDING으로 저장되며, 대상(Recipe/Dataset)의 검증 상태는
     *   별도 컬럼으로 중복 저장하지 않고 VerificationResultService가 이 status를 조회 시점에 읽어 파생시킨다.
     * - 실제 검수 처리(점수/코멘트 기록)는 전문가/관리자가 이후 status를 APPROVED/REJECTED로 변경하는 별도 플로우에서 이루어진다.
     */
    @Override
    public InspectionResponse createInspection(User user, InspectionCreateRequest request) {
        InspectionTargetType targetType = request.getTargetType();
        Long targetId = request.getTargetId();
        Integer rewardPoints = request.getRewardPoints();

        validateTargetOwnership(user, targetType, targetId);

        if (expertVerificationRepository.existsByUserAndTargetTypeAndTargetId(user, targetType, targetId)) {
            throw new BusinessException(ErrorCode.INSPECTION_ALREADY_EXISTS);
        }

        int currentPoints = user.getPoints() == null ? 0 : user.getPoints();
        if (currentPoints < rewardPoints) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINTS);
        }
        user.deductPoints(rewardPoints);

        ExpertVerification verification = ExpertVerification.create(user, targetType, targetId, request.getReason(), rewardPoints);
        try {
            ExpertVerification saved = expertVerificationRepository.save(verification);
            return InspectionResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 existsBy 체크를 통과한 뒤 unique constraint에 걸린 경우
            throw new BusinessException(ErrorCode.INSPECTION_ALREADY_EXISTS);
        }
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
