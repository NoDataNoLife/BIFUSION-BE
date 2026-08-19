package com.nodatanolife.bifusion_be.domain.recipe.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 레시피 댓글/대댓글 작성 요청 DTO.
 */
public record RecipeReviewRequest(
        // 최상위 리뷰는 필수, 대댓글은 보내면 안 된다. parentReviewId에 따라 갈리므로 서비스에서 검증한다.
        @Min(1) @Max(5)
        Integer rating,

        // 댓글 내용 (필수)
        @NotBlank
        String content,

        // null이면 최상위 리뷰, 값이 있으면 해당 리뷰의 대댓글
        Long parentReviewId
) {
    /**
     * 대댓글 요청 여부. parentReviewId가 있으면 대댓글이다.
     */
    public boolean isReply() {
        return parentReviewId != null;
    }

    /**
     * 엔티티 저장용으로 rating을 Short로 변환한다. null은 그대로 유지.
     */
    public Short ratingAsShort() {
        return rating == null ? null : rating.shortValue();
    }
}
