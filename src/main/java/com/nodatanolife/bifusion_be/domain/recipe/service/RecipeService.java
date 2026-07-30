package com.nodatanolife.bifusion_be.domain.recipe.service;

import com.nodatanolife.bifusion_be.domain.recipe.dto.response.RecipeListResponse;
import com.nodatanolife.bifusion_be.domain.recipe.dto.request.RecipeRegisterRequest;
import com.nodatanolife.bifusion_be.domain.recipe.dto.response.RecipeRegisterResponse;
import com.nodatanolife.bifusion_be.domain.user.entity.User;

public interface RecipeService {

    void anonymizeByUser(User user, User deletedUser);

    RecipeRegisterResponse registerRecipe(RecipeRegisterRequest request, Long userId);

    RecipeListResponse getRecipeList(String sort, int page, int size);

}
