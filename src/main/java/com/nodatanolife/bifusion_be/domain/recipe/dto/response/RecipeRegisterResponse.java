package com.nodatanolife.bifusion_be.domain.recipe.dto.response;

import com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe;
import java.time.LocalDateTime;

/**
 * 레시피 등록(일반/포크 공통) 응답 DTO.
 * isPublic을 함께 내려주어 프론트가 어떤 공개 범위로 저장됐는지 즉시 확인할 수 있도록 한다.
 */
public record RecipeRegisterResponse(
        // 화면 표시용 접두어("REC-")가 붙은 레시피 ID
        String recipeId,
        Boolean isPublic,
        LocalDateTime createdAt
) {
    public static RecipeRegisterResponse from(Recipe recipe) {
        return new RecipeRegisterResponse(
                "REC-" + recipe.getRecipeId(),
                recipe.getIsPublic(),
                recipe.getCreatedAt()
        );
    }
}