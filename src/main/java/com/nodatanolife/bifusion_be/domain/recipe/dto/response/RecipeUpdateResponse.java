package com.nodatanolife.bifusion_be.domain.recipe.dto.response;

public record RecipeUpdateResponse(
        Long recipeId
) {
    public static RecipeUpdateResponse of(Long recipeId) {
        return new RecipeUpdateResponse(recipeId);
    }
}