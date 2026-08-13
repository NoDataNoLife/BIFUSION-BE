package com.nodatanolife.bifusion_be.domain.recipe.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RecipeReviewUpdateRequest(
        // 최상위 댓글은 필수, 대댓글은 보내면 안 된다. 대상 댓글이 최상위인지 대댓글인지는 서비스에서 검증한다.
        @Min(1) @Max(5)
        Integer rating,

        @NotBlank
        String content
) {
    public Short ratingAsShort() {
        return rating == null ? null : rating.shortValue();
    }
}
