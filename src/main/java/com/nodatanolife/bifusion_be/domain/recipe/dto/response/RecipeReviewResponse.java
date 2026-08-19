package com.nodatanolife.bifusion_be.domain.recipe.dto.response;

import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
import com.nodatanolife.bifusion_be.domain.recipe.entity.RecipeReview;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 레시피 댓글/대댓글 응답 DTO.
 * 최상위 댓글 응답에는 딸린 대댓글 목록(replies)이 함께 채워지고,
 * 대댓글 자신의 응답에서는 replies가 항상 빈 리스트다.
 */
public record RecipeReviewResponse(
        Long reviewId,
        Long userId,
        String author,
        // S3 버킷이 private이라 직렬화 시점에 presign된다
        @PresignedUrl String profileImageUrl,
        LocalDateTime createdAt,
        String content,
        // 최상위 댓글이면 null, 대댓글이면 부모 댓글 ID
        Long parentReviewId,
        List<RecipeReviewResponse> replies
) {
    /**
     * 대댓글이 없는 단건 변환 (대댓글 응답 변환 시 사용).
     */
    public static RecipeReviewResponse from(RecipeReview review) {
        return from(review, List.of());
    }

    /**
     * 딸린 대댓글 목록과 함께 변환 (최상위 댓글 응답 변환 시 사용).
     */
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
