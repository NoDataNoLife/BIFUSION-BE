package com.nodatanolife.bifusion_be.domain.recipe.controller;

import com.nodatanolife.bifusion_be.domain.recipe.dto.response.RecipeDetailResponse;
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

    @Operation(
        summary = "레시피 등록",
        description = "새 레시피를 등록합니다. 일반 등록과 포크 등록이 이 API로 공동 처리됩니다.\n\n"
                + "- 일반 등록: forkedFromId 없이 요청\n"
                + "- 포크 등록: 포크 원본 정보는 레시피 상세 조회 API(GET)로 미리 조회하여 "
                + "프론트에서 값을 채운 뒤, forkedFromId에 원본 레시피 ID를 포함하여 요청. "
                + "등록 시 원본 레시피의 forkCount가 자동으로 증가합니다."
    )
    @PostMapping
    public CommonResponse<RecipeRegisterResponse> registerRecipe(
        @AuthenticationPrincipal CustomOAuth2User oAuth2User,
        @RequestBody @Valid RecipeRegisterRequest request
    ) {
        Long userId = oAuth2User.getUser().getUserId();
        RecipeRegisterResponse response = recipeService.registerRecipe(request, userId);
        
        return CommonResponse.success(response, request.forkedFromId() != null ? "포크 레시피 등록 성공!" : "레시피 등록 성공!");
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

    @Operation(summary = "레시피 상세내역 조회", description = "레시피 ID로 상세 정보를 조회합니다.")
    @GetMapping("/{recipeId}")
    public CommonResponse<RecipeDetailResponse> getRecipeDetail(
            @PathVariable Long recipeId
    ) {
        RecipeDetailResponse response = recipeService.getRecipeDetail(recipeId);
        return CommonResponse.success(response, "레시피 상세 조회 성공!");
    }

}
