package com.nodatanolife.bifusion_be.domain.recipe.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RecipeReviewRequest(
        // 최상위 리뷰는 필수, 대댓글은 보내면 안 된다. parentReviewId에 따라 갈리므로 서비스에서 검증한다.
        @Min(1) @Max(5)
        Integer rating,

        @NotBlank
        String content,

        // null이면 최상위 리뷰, 값이 있으면 해당 리뷰의 대댓글
        Long parentReviewId
) {
    public boolean isReply() {
        return parentReviewId != null;
    }

    public Short ratingAsShort() {
        return rating == null ? null : rating.shortValue();
    }
}
