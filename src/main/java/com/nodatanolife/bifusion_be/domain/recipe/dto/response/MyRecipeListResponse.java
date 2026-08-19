package com.nodatanolife.bifusion_be.domain.recipe.dto.response;

import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionSummary;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionStatus;
import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
import com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe;

/**
 * "내 자산" 레시피 목록 조회 응답 DTO.
 * RecipeRepository의 findAllByForkedFromIsNull.../findAllByForkedFromIsNotNull...이
 * 공개 여부와 무관하게 본인 소유 레시피를 전부 조회하므로, 공개/비공개 레시피가 함께 담길 수 있다.
 */
public record MyRecipeListResponse(
        Long recipeId,
        String title,
        String description,
        Boolean expertVerified,
        Double rating,
        Long forkCount,
        Long viewCount,
        @PresignedUrl String bannerImageUrl
) {
    public static MyRecipeListResponse from(Recipe recipe, InspectionSummary verificationSummary) {
        return new MyRecipeListResponse(
                recipe.getRecipeId(),
                recipe.getTitle(),
                recipe.getShortDescription(),
                verificationSummary.status() == InspectionStatus.COMPLETED,
                recipe.getRating().doubleValue(),
                recipe.getForkCount().longValue(),
                recipe.getViewCount().longValue(),
                recipe.getThumbnailUrl()
        );
    }
}
