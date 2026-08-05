package com.nodatanolife.bifusion_be.domain.recipe.dto.response;

public record RecipeForkResponse(
        Long newRecipeId,
        Long originalRecipeId
) {
    public static RecipeForkResponse of(Long newRecipeId, Long originalRecipeId) {
        return new RecipeForkResponse(newRecipeId, originalRecipeId);
    }
}