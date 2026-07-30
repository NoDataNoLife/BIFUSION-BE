package com.nodatanolife.bifusion_be.domain.recipe.controller;

import com.nodatanolife.bifusion_be.domain.recipe.dto.response.RecipeListResponse;
import com.nodatanolife.bifusion_be.domain.recipe.dto.request.RecipeRegisterRequest;
import com.nodatanolife.bifusion_be.domain.recipe.dto.response.RecipeRegisterResponse;
import com.nodatanolife.bifusion_be.domain.recipe.service.RecipeService;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Recipe", description = "레시피 API")
@RestController
@RequestMapping("/api/v1/community/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    @Operation(summary = "새 레시피 등록", description = "새로운 레시피를 등록합니다.")
    @PostMapping
    public CommonResponse<RecipeRegisterResponse> registerRecipe(
            @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @RequestBody @Valid RecipeRegisterRequest request
    ) {
        Long userId = oAuth2User.getUser().getUserId();

        RecipeRegisterResponse response = recipeService.registerRecipe(request, userId);

        return CommonResponse.success(response, "새 레시피가 성공적으로 등록되었습니다.");
    }

    @Operation(summary = "레시피 목록 전체 조회", description = "sort(LATEST/MOST_POPULAR/MOST_FORKED), page, size 파라미터로 조회합니다.")
    @GetMapping
    public CommonResponse<RecipeListResponse> getRecipeList(
            @RequestParam(defaultValue = "LATEST") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        RecipeListResponse response = recipeService.getRecipeList(sort, page, size);
        return CommonResponse.success(response, "레시피 목록 조회 성공!");
    }
}