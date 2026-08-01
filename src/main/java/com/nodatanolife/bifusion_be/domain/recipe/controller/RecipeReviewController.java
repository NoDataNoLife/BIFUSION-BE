package com.nodatanolife.bifusion_be.domain.recipe.controller;

import com.nodatanolife.bifusion_be.domain.recipe.dto.request.RecipeReviewRequest;
import com.nodatanolife.bifusion_be.domain.recipe.dto.response.RecipeReviewResponse;
import com.nodatanolife.bifusion_be.domain.recipe.service.RecipeReviewService;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Recipe Review", description = "레시피 리뷰 API")
@RestController
@RequestMapping("/api/v1/community/recipes")
@RequiredArgsConstructor
public class RecipeReviewController {

    private final RecipeReviewService recipeReviewService;

    @Operation(summary = "리뷰 작성", description = "레시피에 대한 리뷰(별점, 내용)를 작성합니다.")
    @PostMapping("/{recipeId}/reviews")
    public CommonResponse<RecipeReviewResponse> createReview(
            @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @PathVariable Long recipeId,
            @RequestBody @Valid RecipeReviewRequest request
    ) {
        Long userId = oAuth2User.getUser().getUserId();

        RecipeReviewResponse response = recipeReviewService.createReview(recipeId, userId, request);

        return CommonResponse.success(response, "리뷰 작성 성공");
    }
}