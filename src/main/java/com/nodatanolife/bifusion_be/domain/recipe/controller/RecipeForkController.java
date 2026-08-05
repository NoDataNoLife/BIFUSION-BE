package com.nodatanolife.bifusion_be.domain.recipe.controller;

import com.nodatanolife.bifusion_be.domain.recipe.dto.response.RecipeForkResponse;
import com.nodatanolife.bifusion_be.domain.recipe.service.RecipeForkService;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Recipe Fork", description = "레시피 포크 API")
@RestController
@RequestMapping("/api/v1/community/recipes")
@RequiredArgsConstructor
public class RecipeForkController {

    private final RecipeForkService recipeForkService;

    @Operation(summary = "레시피 포크", description = "원본 레시피를 복사해 내 보관함에 저장합니다.")
    @PostMapping("/{recipeId}/fork")
    public CommonResponse<RecipeForkResponse> forkRecipe(
            @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @PathVariable Long recipeId
    ) {
        Long userId = oAuth2User.getUser().getUserId();

        RecipeForkResponse response = recipeForkService.forkRecipe(recipeId, userId);

        return CommonResponse.success(response, "내 레시피 보관함에 저장 성공!");
    }
}