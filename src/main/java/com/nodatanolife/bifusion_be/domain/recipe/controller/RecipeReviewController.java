package com.nodatanolife.bifusion_be.domain.recipe.controller;

import com.nodatanolife.bifusion_be.domain.recipe.dto.request.RecipeReviewRequest;
import com.nodatanolife.bifusion_be.domain.recipe.dto.request.RecipeReviewUpdateRequest;
import com.nodatanolife.bifusion_be.domain.recipe.dto.response.RecipeReviewResponse;
import com.nodatanolife.bifusion_be.domain.recipe.service.RecipeReviewService;
import com.nodatanolife.bifusion_be.global.dto.response.PageResponse;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Recipe Review", description = "레시피 댓글 API")
@RestController
@RequestMapping("/api/v1/community/recipes")
@RequiredArgsConstructor
public class RecipeReviewController {

    private final RecipeReviewService recipeReviewService;

    @Operation(
            summary = "댓글/대댓글 작성",
            description = "레시피에 댓글을 작성합니다. parentReviewId가 null이면 최상위 댓글로 등록되며 "
                    + "이 경우 rating(1~5)이 필수입니다. parentReviewId에 값을 넣으면 해당 댓글의 대댓글로 등록되며 "
                    + "이 경우 rating은 보낼 수 없습니다(대댓글에는 다시 답글을 달 수 없어 최대 depth는 1단계입니다)."
    )
    @PostMapping("/{recipeId}/reviews")
    public CommonResponse<RecipeReviewResponse> createReview(
            @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @PathVariable Long recipeId,
            @RequestBody @Valid RecipeReviewRequest request
    ) {
        Long userId = oAuth2User.getUser().getUserId();

        RecipeReviewResponse response = recipeReviewService.createReview(recipeId, userId, request);

        return CommonResponse.success(response, request.isReply() ? "대댓글 작성 성공!" : "댓글 작성 성공!");
    }

   @Operation(
            summary = "댓글/대댓글 수정",
            description = "본인이 작성한 레시피 댓글을 수정합니다. 최상위 댓글은 rating(1~5)이 필수이며, "
                    + "대댓글은 rating을 보낼 수 없습니다. 작성자 본인이 아니면 수정이 거부됩니다."
    )
    @PutMapping("/{recipeId}/reviews/{reviewId}")
    public CommonResponse<RecipeReviewResponse> updateReview(
            @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @PathVariable Long recipeId,
            @PathVariable Long reviewId,
            @RequestBody @Valid RecipeReviewUpdateRequest request
    ) {
        Long userId = oAuth2User.getUser().getUserId();
        RecipeReviewResponse response = recipeReviewService.updateReview(recipeId, reviewId, userId, request);
        return CommonResponse.success(response, response.parentReviewId() != null ? "대댓글 수정 성공!" : "댓글 수정 성공!");
    }

    @Operation(
            summary = "댓글/대댓글 삭제",
            description = "본인이 작성한 레시피 댓글을 삭제합니다. 작성자 본인이 아니면 삭제가 거부됩니다. "
                    + "하드 삭제이며, 최상위 댓글을 삭제하면 딸린 대댓글도 함께 삭제됩니다."
    )
    @DeleteMapping("/{recipeId}/reviews/{reviewId}")
    public CommonResponse<Void> deleteReview(
            @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @PathVariable Long recipeId,
            @PathVariable Long reviewId
    ) {
        Long userId = oAuth2User.getUser().getUserId();
        boolean isReply = recipeReviewService.deleteReview(recipeId, reviewId, userId);
        return CommonResponse.success(null, isReply ? "대댓글 삭제 성공!" : "댓글 삭제 성공!");
    }

    @Operation(
            summary = "댓글/대댓글 목록 조회",
            description = "레시피의 최상위 댓글을 생성순(오래된 순)으로 페이지 조회합니다. "
                    + "각 최상위 댓글에는 딸린 대댓글이 생성순으로 함께 내려갑니다. page, size 파라미터로 페이징합니다."
    )
    @GetMapping("/{recipeId}/reviews")
    public CommonResponse<PageResponse<RecipeReviewResponse>> getReviews(
            @PathVariable Long recipeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<RecipeReviewResponse> response = PageResponse.from(recipeReviewService.getReviews(recipeId, page, size));
        return CommonResponse.success(response, "댓글 목록 조회 성공!");
    }
}