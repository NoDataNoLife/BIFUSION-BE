package com.nodatanolife.bifusion_be.domain.recipe.controller;

import com.nodatanolife.bifusion_be.domain.recipe.dto.response.RecipeDetailResponse;
import com.nodatanolife.bifusion_be.domain.recipe.dto.response.RecipeForkResponse;
import com.nodatanolife.bifusion_be.domain.recipe.dto.response.RecipeListResponse;
import com.nodatanolife.bifusion_be.domain.recipe.dto.request.RecipeRegisterRequest;
import com.nodatanolife.bifusion_be.domain.recipe.dto.response.RecipeRegisterResponse;
import com.nodatanolife.bifusion_be.domain.recipe.service.RecipeForkService;
import com.nodatanolife.bifusion_be.domain.recipe.service.RecipeService;
import com.nodatanolife.bifusion_be.global.dto.response.PageResponse;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Community Recipe", description = "커뮤니티 레시피 API")
@RestController
@RequestMapping("/api/v1/community/recipes")
@RequiredArgsConstructor
public class CommunityRecipeController {

    private final RecipeService recipeService;
    private final RecipeForkService recipeForkService;

    @Operation(
        summary = "레시피 등록",
        description = """
                새 레시피를 등록합니다. 일반 등록과 포크 등록이 이 API로 공동 처리됩니다.

                - 일반 등록: forkedFromId 없이 요청
                - 포크 등록: 포크 원본 정보는 레시피 상세 조회 API(GET)로 미리 조회하여 \
                프론트에서 값을 채운 뒤, forkedFromId에 원본 레시피 ID를 포함하여 요청. \
                등록 시 원본 레시피의 forkCount가 자동으로 증가합니다.
                - isPublic(true: 커뮤니티 공개 / false: 내 자산 전용 비공개) 값으로 공개 범위를 지정하며, \
                생략하면 기존과 동일하게 공개(true)로 등록됩니다.
                - thumbnailFileId(선택): 대표 이미지. POST /api/v1/files/temp로 올린 본인 이미지 파일의 fileId. \
                없는 파일 404, 남의 파일 거부, 이미지가 아니면 400(CM023). 응답의 bannerUrl로 내려갑니다."""
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


    @Operation(
            summary = "커뮤니티 레시피 목록 조회",
            description = "커뮤니티에 공개(isPublic=true)된 레시피만 조회합니다. "
                    + "sort(LATEST: 최신순 / MOST_POPULAR: 좋아요순 / MOST_FORKED: 포크순), "
                    + "keyword(제목·짧은 설명 부분 검색, 생략 가능), page, size 파라미터로 조회합니다."
    )
    @GetMapping
    public CommonResponse<PageResponse<RecipeListResponse>> getRecipeList(
            @RequestParam(defaultValue = "LATEST") String sort,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<RecipeListResponse> response = PageResponse.from(
                recipeService.getRecipeList(sort, keyword, page, size)
        );
        return CommonResponse.success(response, "레시피 목록 조회 성공!");
    }

    @Operation(
            summary = "레시피 상세내역 조회",
            description = "레시피 ID로 상세 정보를 조회합니다. isPublic=false인 비공개 레시피(포크로 생성된 "
                    + "내 보관함 사본 포함)는 작성자 본인만 조회할 수 있으며, 본인이 아닌 사용자가 요청하면 접근이 거부됩니다. "
                    + "reviewCount는 최상위 리뷰 + 대댓글 합산입니다 (리뷰 목록 API의 totalElements는 최상위 리뷰 수)."
    )
    @GetMapping("/{recipeId}")
    public CommonResponse<RecipeDetailResponse> getRecipeDetail(
            @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @PathVariable Long recipeId
    ) {
        Long userId = oAuth2User.getUser().getUserId();
        RecipeDetailResponse response = recipeService.getRecipeDetail(recipeId, userId);
        return CommonResponse.success(response, "레시피 상세 조회 성공!");
    }

    @Operation(
            summary = "레시피 포크",
            description = "원본 레시피를 복제해 요청자 본인의 \"내 보관함\"에 저장합니다. "
                    + "생성되는 사본은 항상 isPublic=false(비공개)로 저장되며, 원본의 forkCount가 1 증가합니다. "
                    + "동일 사용자가 같은 원본을 이미 포크한 상태에서 다시 요청하면 실패합니다(중복 포크 불가)."
    )
    @PostMapping("/{recipeId}/fork")
    public CommonResponse<RecipeForkResponse> forkRecipe(
            @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @PathVariable Long recipeId
    ) {
        Long userId = oAuth2User.getUser().getUserId();

        RecipeForkResponse response = recipeForkService.forkRecipe(recipeId, userId);

        return CommonResponse.success(response, "내 레시피 보관함에 저장 성공!");
    }

    @Operation(
            summary = "레시피 삭제",
            description = """
            지정한 레시피를 삭제합니다(soft delete). 작성자 본인만 삭제할 수 있으며, \
            본인이 아닌 사용자가 요청하면 접근이 거부됩니다(403).

            삭제 시 다음과 같이 처리됩니다.
            - 전문가가 아직 시작하지 않은 검수 신청은 자동으로 취소되고 포인트가 환불됩니다.
            - 전문가가 검수를 시작한 레시피는 삭제할 수 없습니다(409, IS012).
            - 삭제 후에는 레시피 목록 조회, 상세 조회, 검색, 포크 조회에서 노출되지 않습니다.
            - 삭제된 레시피에는 리뷰를 조회하거나 새로 작성할 수 없으며, 요청 시 404로 처리됩니다.
            - 삭제된 레시피는 마이페이지 노출 설정 변경 대상에서 제외되며, 요청 시 404(ACTIVITY_NOT_FOUND)로 처리됩니다.
            - 삭제된 레시피는 전문가 검수 신청 대상에서 제외되며, 요청 시 404(INSPECTION_TARGET_NOT_FOUND)로 처리됩니다."""
    )
    @DeleteMapping("/{recipeId}")
    public CommonResponse<Void> deleteRecipe(
            @PathVariable Long recipeId,
            @AuthenticationPrincipal CustomOAuth2User oAuth2User
    ) {
        recipeService.deleteRecipe(recipeId, oAuth2User.getUser().getUserId());

        return CommonResponse.success(null, "레시피 삭제 성공!");
    }

    @Operation(
            summary = "레시피 수정",
            description = "본인이 작성한 레시피를 수정합니다."
    )
    @PutMapping("/{recipeId}")
    public CommonResponse<RecipeRegisterResponse> updateRecipe(
            @PathVariable Long recipeId,
            @RequestBody @Valid RecipeRegisterRequest request,
            @AuthenticationPrincipal CustomOAuth2User oAuth2User
    ) {
        Long userId = oAuth2User.getUser().getUserId();
        RecipeRegisterResponse response = recipeService.updateRecipe(recipeId, request, userId);
        return CommonResponse.success(response, "레시피 수정 성공!");
    }

}
