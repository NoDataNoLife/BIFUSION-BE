package com.nodatanolife.bifusion_be.domain.recipe.dto.response;

import com.nodatanolife.bifusion_be.domain.recipe.entity.RecipeReview;

import java.time.LocalDate;

public record RecipeReviewResponse(
        Long reviewId,
        LocalDate createdAt
) {
    public static RecipeReviewResponse from(RecipeReview review) {
        return new RecipeReviewResponse(
                review.getRecipeReviewId(),
                review.getCreatedAt().toLocalDate()
        );
    }
}