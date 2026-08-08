package com.nodatanolife.bifusion_be.domain.recipe.dto.response;

import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
import com.nodatanolife.bifusion_be.domain.recipe.entity.RecipeReview;

import java.time.LocalDateTime;
import java.util.List;

public record RecipeReviewResponse(
        Long reviewId,
        Long userId,
        String author,
        @PresignedUrl String profileImageUrl,
        LocalDateTime createdAt,
        String content,
        Long parentReviewId,
        List<RecipeReviewResponse> replies
) {
    public static RecipeReviewResponse from(RecipeReview review) {
        return from(review, List.of());
    }

    public static RecipeReviewResponse from(RecipeReview review, List<RecipeReviewResponse> replies) {
        return new RecipeReviewResponse(
                review.getRecipeReviewId(),
                review.getUser().getUserId(),
                review.getUser().getNickname(),
                review.getUser().getProfileImageUrl(),
                review.getCreatedAt(),
                review.getContent(),
                review.isReply() ? review.getParent().getRecipeReviewId() : null,
                replies
        );
    }
}
