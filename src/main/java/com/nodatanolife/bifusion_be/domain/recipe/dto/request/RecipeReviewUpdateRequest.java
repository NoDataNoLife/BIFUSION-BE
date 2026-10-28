package com.nodatanolife.bifusion_be.domain.recipe.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 레시피 댓글/대댓글 수정 요청 DTO.
 */
public record RecipeReviewUpdateRequest(
        // 최상위 댓글은 필수, 대댓글은 보내면 안 된다. 대상 댓글이 최상위인지 대댓글인지는 서비스에서 검증한다.
        @Min(1) @Max(5)
        Integer rating,

        // 댓글 내용 (필수)
        @NotBlank
        @Size(max = 2000, message = "댓글은 2000자 이하로 입력해 주세요.")
        String content
) {
    /**
     * 엔티티 갱신용으로 rating을 Short로 변환한다. null은 그대로 유지.
     */
    public Short ratingAsShort() {
        return rating == null ? null : rating.shortValue();
    }
}
