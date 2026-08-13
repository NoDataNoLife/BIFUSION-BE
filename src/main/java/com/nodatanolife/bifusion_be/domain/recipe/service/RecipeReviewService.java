package com.nodatanolife.bifusion_be.domain.recipe.service;

import com.nodatanolife.bifusion_be.domain.recipe.dto.request.RecipeReviewRequest;
import com.nodatanolife.bifusion_be.domain.recipe.dto.request.RecipeReviewUpdateRequest;
import com.nodatanolife.bifusion_be.domain.recipe.dto.response.RecipeReviewResponse;
import org.springframework.data.domain.Page;

public interface RecipeReviewService {
    RecipeReviewResponse createReview(Long recipeId, Long userId, RecipeReviewRequest request);

    boolean deleteReview(Long recipeId, Long reviewId, Long userId);
    
    RecipeReviewResponse updateReview(Long recipeId, Long reviewId, Long userId, RecipeReviewUpdateRequest request);
  
    Page<RecipeReviewResponse> getReviews(Long recipeId, int page, int size);
}