package com.nodatanolife.bifusion_be.domain.recipe.service;

import com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe;
import java.util.List;
import com.nodatanolife.bifusion_be.domain.recipe.dto.response.MyRecipeListResponse;
import com.nodatanolife.bifusion_be.domain.recipe.dto.response.RecipeDetailResponse;
import com.nodatanolife.bifusion_be.domain.recipe.dto.response.RecipeListResponse;
import com.nodatanolife.bifusion_be.domain.recipe.dto.request.RecipeRegisterRequest;
import com.nodatanolife.bifusion_be.domain.recipe.dto.response.RecipeRegisterResponse;
import com.nodatanolife.bifusion_be.domain.recipe.entity.RecipeListType;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.domain.Page;

public interface RecipeService {

    /**
     * 회원 탈퇴 시 해당 유저가 작성한 모든 레시피/리뷰/포크이력의 작성자를 탈퇴 계정으로 치환한다.
     */
    void anonymizeByUser(User user, User deletedUser);

    /**
     * 레시피를 등록한다. request.isPublic()에 따라 공개(커뮤니티 노출) 또는
     * 비공개(내 자산 전용)로 저장되며, forkedFromId가 있으면 포크 등록으로 처리된다.
     */
    RecipeRegisterResponse registerRecipe(RecipeRegisterRequest request, Long userId);

    /**
     * 레시피 상세를 조회한다. 비공개 레시피면 작성자 본인(userId)만 조회할 수 있다.
     */
    RecipeDetailResponse getRecipeDetail(Long recipeId, Long userId);

    /**
     * 내 자산 레시피 목록을 조회한다. 공개 여부와 무관하게 본인 소유 레시피를
     * type(MINE: 직접 작성 / FORKED: 포크해온 것)으로 필터링한다.
     */
    Page<MyRecipeListResponse> getMyRecipes(RecipeListType type, Long userId, int page, int size);

    /**
     * 커뮤니티 레시피 목록을 조회한다. 공개(isPublic = true) 레시피만 대상이다.
     * 페이징은 컨트롤러에서 전역 PageResponse로 감싼다.
     */
    Page<RecipeListResponse> getRecipeList(String sort, String keyword, int page, int size);

    void deleteRecipe(Long recipeId, Long userId);

    List<Recipe> getPublicRecipesByUser(Long userId);

    RecipeRegisterResponse updateRecipe(Long recipeId, RecipeRegisterRequest request, Long userId);
}
