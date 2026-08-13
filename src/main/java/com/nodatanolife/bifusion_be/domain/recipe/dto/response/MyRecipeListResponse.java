package com.nodatanolife.bifusion_be.domain.recipe.dto.response;

import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
import com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe;

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
    public static MyRecipeListResponse from(Recipe recipe) {
        return new MyRecipeListResponse(
                recipe.getRecipeId(),
                recipe.getTitle(),
                recipe.getShortDescription(),
                recipe.getIsExpertVerified(),
                recipe.getRating().doubleValue(),
                recipe.getForkCount().longValue(),
                recipe.getViewCount().longValue(),
                recipe.getThumbnailUrl()
        );
    }
}
