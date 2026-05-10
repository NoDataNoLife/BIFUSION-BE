package com.nodatanolife.bifusion_be.domain.user.service;

import com.nodatanolife.bifusion_be.domain.dataset.entity.Dataset;
import com.nodatanolife.bifusion_be.domain.dataset.repository.DatasetRepository;
import com.nodatanolife.bifusion_be.domain.qna.entity.ExpertQnA;
import com.nodatanolife.bifusion_be.domain.qna.repository.ExpertQnARepository;
import com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe;
import com.nodatanolife.bifusion_be.domain.recipe.repository.RecipeRepository;
import com.nodatanolife.bifusion_be.domain.recruitment.entity.Recruit;
import com.nodatanolife.bifusion_be.domain.recruitment.repository.RecruitRepository;
import com.nodatanolife.bifusion_be.domain.user.dto.request.ActivityVisibilityUpdateRequest;
import com.nodatanolife.bifusion_be.domain.user.dto.response.ActivityVisibilityUpdateResponse;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MyActivityVisibilityServiceImpl implements MyActivityVisibilityService {

    private final RecipeRepository recipeRepository;
    private final DatasetRepository datasetRepository;
    private final ExpertQnARepository expertQnARepository;
    private final RecruitRepository recruitRepository;

    /**
     * 마이페이지 활동 공개 여부 일괄 변경
     * - 본인 소유가 아닌 항목이 하나라도 있으면 전체 롤백 (ACTIVITY_ACCESS_DENIED)
     * - 비공개 콘텐츠(isPublic=false)에 공개 요청이 들어오면 전체 롤백 (ACTIVITY_PRIVATE_CONTENT_CANNOT_BE_PUBLIC)
     */
    @Override
    public ActivityVisibilityUpdateResponse updateVisibilities(
            Long userId,
            ActivityVisibilityUpdateRequest request
    ) {
        List<ActivityVisibilityUpdateResponse.Item> updated = new ArrayList<>();

        for (ActivityVisibilityUpdateRequest.Item item : request.items()) {
            boolean visible = Boolean.TRUE.equals(item.isPublic());
            Boolean savedValue = switch (item.type()) {
                case RECIPE -> updateRecipe(item.id(), userId, visible);
                case DATASET -> updateDataset(item.id(), userId, visible);
                case QNA -> updateQnA(item.id(), userId, visible);
                case RECRUITMENT -> updateRecruit(item.id(), userId, visible);
            };

            updated.add(new ActivityVisibilityUpdateResponse.Item(
                    item.type(),
                    item.id(),
                    savedValue
            ));
        }

        return new ActivityVisibilityUpdateResponse(updated);
    }

    /**
     * 마이페이지 활동 공개 여부 일괄 변경 (레시피)
     */
    private Boolean updateRecipe(Long recipeId, Long userId, boolean visible) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND));
        if (!recipe.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACTIVITY_ACCESS_DENIED);
        }
        if (visible && !Boolean.TRUE.equals(recipe.getIsPublic())) {
            throw new BusinessException(ErrorCode.ACTIVITY_PRIVATE_CONTENT_CANNOT_BE_PUBLIC);
        }
        recipe.updateMyPageVisibility(visible);
        return recipe.getIsMyPageVisible();
    }

    /**
     * 마이페이지 활동 공개 여부 일괄 변경 (데이터셋)
     */
    private Boolean updateDataset(Long datasetId, Long userId, boolean visible) {
        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND));
        if (!dataset.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACTIVITY_ACCESS_DENIED);
        }
        if (visible && !Boolean.TRUE.equals(dataset.getIsPublic())) {
            throw new BusinessException(ErrorCode.ACTIVITY_PRIVATE_CONTENT_CANNOT_BE_PUBLIC);
        }
        dataset.updateMyPageVisibility(visible);
        return dataset.getIsMyPageVisible();
    }

    /**
     * 마이페이지 활동 공개 여부 일괄 변경 (큐엔에이)
     */
    private Boolean updateQnA(Long qnaId, Long userId, boolean visible) {
        ExpertQnA qna = expertQnARepository.findById(qnaId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND));
        if (!qna.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACTIVITY_ACCESS_DENIED);
        }
        if (visible && !Boolean.TRUE.equals(qna.getIsPublic())) {
            throw new BusinessException(ErrorCode.ACTIVITY_PRIVATE_CONTENT_CANNOT_BE_PUBLIC);
        }
        qna.updateMyPageVisibility(visible);
        return qna.getIsMyPageVisible();
    }

    /**
     * 마이페이지 활동 공개 여부 일괄 변경 (팀원 모집)
     */
    private Boolean updateRecruit(Long recruitId, Long userId, boolean visible) {
        Recruit recruit = recruitRepository.findById(recruitId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND));
        if (!recruit.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACTIVITY_ACCESS_DENIED);
        }
        if (visible && !Boolean.TRUE.equals(recruit.getIsPublic())) {
            throw new BusinessException(ErrorCode.ACTIVITY_PRIVATE_CONTENT_CANNOT_BE_PUBLIC);
        }
        recruit.updateMyPageVisibility(visible);
        return recruit.getIsMyPageVisible();
    }
}
