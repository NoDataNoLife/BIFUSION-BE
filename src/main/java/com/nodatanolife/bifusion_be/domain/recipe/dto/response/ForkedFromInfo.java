package com.nodatanolife.bifusion_be.domain.recipe.dto.response;

import com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe;

/**
 * 포크한 레시피의 원본 정보. 사본 화면의 "원본: ○○" 링크용이며, 링크는 recipeId로 레시피 상세 경로를 만든다.
 * 원본이 삭제됐으면 isDeleted=true (링크를 막는다).
 */
public record ForkedFromInfo(
        Long recipeId,
        String title,
        String authorNickname,
        boolean isDeleted
) {
    /**
     * @return 직접 작성한 레시피(포크 아님)면 null
     */
    public static ForkedFromInfo of(Recipe recipe) {
        Recipe original = recipe.getForkedFrom();
        if (original == null) {
            return null;
        }
        return new ForkedFromInfo(
                original.getRecipeId(),
                original.getTitle(),
                original.getUser().getNickname(),
                original.isDeleted()
        );
    }
}
