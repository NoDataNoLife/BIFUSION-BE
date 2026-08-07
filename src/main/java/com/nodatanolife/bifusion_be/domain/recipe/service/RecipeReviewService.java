package com.nodatanolife.bifusion_be.domain.recipe.service;

import com.nodatanolife.bifusion_be.domain.recipe.dto.request.RecipeReviewRequest;
import com.nodatanolife.bifusion_be.domain.recipe.dto.response.RecipeReviewResponse;

public interface RecipeReviewService {
    RecipeReviewResponse createReview(Long recipeId, Long userId, RecipeReviewRequest request);

    boolean deleteReview(Long recipeId, Long reviewId, Long userId);
}