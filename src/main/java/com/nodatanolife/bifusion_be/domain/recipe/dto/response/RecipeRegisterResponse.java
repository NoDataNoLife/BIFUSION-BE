package com.nodatanolife.bifusion_be.domain.recipe.dto.response;

import com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe;
import java.time.LocalDateTime;

public record RecipeRegisterResponse(
        String recipeId,
        LocalDateTime createdAt
) {
    public static RecipeRegisterResponse from(Recipe recipe) {
        return new RecipeRegisterResponse(
                "REC-" + recipe.getRecipeId(),
                recipe.getCreatedAt()
        );
    }
}