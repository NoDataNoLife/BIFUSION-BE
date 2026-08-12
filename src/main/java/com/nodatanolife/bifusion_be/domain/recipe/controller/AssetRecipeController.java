package com.nodatanolife.bifusion_be.domain.recipe.controller;

import com.nodatanolife.bifusion_be.domain.recipe.dto.response.MyRecipeListResponse;
import com.nodatanolife.bifusion_be.domain.recipe.entity.RecipeListType;
import com.nodatanolife.bifusion_be.domain.recipe.service.RecipeService;
import com.nodatanolife.bifusion_be.global.dto.response.PageResponse;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Assets Recipe", description = "에셋 레시피 API")
@RestController
@RequestMapping("/api/v1/assets/recipes")
@RequiredArgsConstructor
public class AssetRecipeController {

    private final RecipeService recipeService;

    @Operation(
            summary = "내 레시피 목록 조회",
            description = "type(MINE: 내가 작성한 레시피 / FORKED: 내가 Fork한 레시피), page, size 파라미터로 조회합니다."
    )
    @GetMapping
    public CommonResponse<PageResponse<MyRecipeListResponse>> getMyRecipeList(
            @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @Parameter(description = "조회 타입", schema = @Schema(allowableValues = {"MINE", "FORKED"}))
            @RequestParam String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = oAuth2User.getUser().getUserId();
        RecipeListType listType = RecipeListType.from(type);

        PageResponse<MyRecipeListResponse> response =
                PageResponse.from(recipeService.getMyRecipes(listType, userId, page, size));

        return CommonResponse.success(response, "레시피 목록 조회 성공!");
    }
}
