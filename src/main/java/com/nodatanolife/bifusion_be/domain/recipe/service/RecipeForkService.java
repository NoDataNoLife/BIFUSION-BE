package com.nodatanolife.bifusion_be.domain.recipe.service;

import com.nodatanolife.bifusion_be.domain.recipe.dto.response.RecipeForkResponse;

public interface RecipeForkService {
    RecipeForkResponse forkRecipe(Long recipeId, Long userId);
}