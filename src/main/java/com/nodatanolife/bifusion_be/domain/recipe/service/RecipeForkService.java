package com.nodatanolife.bifusion_be.domain.recipe.service;

import com.nodatanolife.bifusion_be.domain.recipe.dto.response.RecipeForkResponse;

public interface RecipeForkService {

    /**
     * 원본 레시피(recipeId)를 복제해 요청자(userId) 소유의 새 레시피로 "내 보관함"에 저장한다.
     * 동일 사용자가 같은 원본을 중복 포크하면 예외가 발생한다.
     */
    RecipeForkResponse forkRecipe(Long recipeId, Long userId);
}