package com.nodatanolife.bifusion_be.domain.recipe.service;

import com.nodatanolife.bifusion_be.domain.recipe.dto.response.MyRecipeListResponse;
import com.nodatanolife.bifusion_be.domain.recipe.dto.response.RecipeDetailResponse;
import com.nodatanolife.bifusion_be.domain.recipe.dto.response.RecipeListResponse;
import com.nodatanolife.bifusion_be.domain.recipe.dto.request.RecipeRegisterRequest;
import com.nodatanolife.bifusion_be.domain.recipe.dto.response.RecipeRegisterResponse;
import com.nodatanolife.bifusion_be.domain.recipe.entity.RecipeListType;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.domain.Page;

public interface RecipeService {

    void anonymizeByUser(User user, User deletedUser);

    RecipeRegisterResponse registerRecipe(RecipeRegisterRequest request, Long userId);

    RecipeListResponse getRecipeList(String sort, int page, int size);

    RecipeDetailResponse getRecipeDetail(Long recipeId);

    Page<MyRecipeListResponse> getMyRecipes(RecipeListType type, Long userId, int page, int size);

}
