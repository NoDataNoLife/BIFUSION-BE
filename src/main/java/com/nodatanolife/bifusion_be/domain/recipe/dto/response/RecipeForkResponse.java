package com.nodatanolife.bifusion_be.domain.recipe.dto.response;

/**
 * 레시피 포크 응답 DTO. 새로 생성된 사본과 포크 원본의 ID를 함께 내려준다.
 */
public record RecipeForkResponse(
        // 포크로 새로 생성된 사본 레시피 ID (내 보관함에 비공개로 저장됨)
        Long newRecipeId,
        // 포크 대상이 된 원본 레시피 ID
        Long originalRecipeId
) {
    public static RecipeForkResponse of(Long newRecipeId, Long originalRecipeId) {
        return new RecipeForkResponse(newRecipeId, originalRecipeId);
    }
}