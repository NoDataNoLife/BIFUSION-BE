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
            description = "로그인한 본인이 작성했거나 포크한 레시피만 조회합니다. 커뮤니티 목록과 달리 "
                    + "isPublic 값(공개/비공개)과 무관하게 본인 소유 레시피가 전부 노출됩니다. "
                    + "type(MINE: forkedFrom이 없는, 내가 직접 작성한 레시피 / FORKED: forkedFrom이 있는, "
                    + "내가 포크해온 레시피), page, size 파라미터로 조회합니다. 등록/상세/포크는 이 컨트롤러가 아니라 "
                    + "커뮤니티 레시피 API(POST·GET /api/v1/community/recipes, POST .../fork)를 그대로 사용합니다."
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
