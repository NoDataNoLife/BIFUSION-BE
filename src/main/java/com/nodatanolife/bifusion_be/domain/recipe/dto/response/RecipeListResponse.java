package com.nodatanolife.bifusion_be.domain.recipe.dto.response;

import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionSummary;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionStatus;
import com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe;
import com.nodatanolife.bifusion_be.domain.user.entity.User;

/**
 * 커뮤니티 레시피 목록의 개별 항목 DTO.
 * 페이징은 전역 {@code PageResponse<T>}(global/dto/response)가 감싸므로
 * 여기서는 totalCount·pageInfo 같은 페이지 메타데이터를 다시 만들지 않고 단건 항목만 정의한다.
 * RecipeRepository#searchRecipes가 이미 isPublic = true인 항목만 조회하므로
 * 이 응답에는 항상 공개 레시피만 담긴다.
 */
public record RecipeListResponse(
        Long recipeId,
        String title,
        String bannerUrl,
        AuthorInfo author,
        int forkCount,
        int likeCount,
        boolean isExpertVerified,
        boolean isDeleted

) {
    /**
     * 작성자 정보.
     */
    public record AuthorInfo(Long userId, String nickname, String profileImageUrl) {
        public static AuthorInfo from(User author) {
            return new AuthorInfo(
                    author.getUserId(),
                    author.getNickname(),
                    author.getProfileImageUrl()
            );
        }
    }

    public static RecipeListResponse from(Recipe recipe, InspectionSummary verificationSummary) {
        return new RecipeListResponse(
                recipe.getRecipeId(),
                recipe.getTitle(),
                recipe.getThumbnailUrl(),
                AuthorInfo.from(recipe.getUser()),
                recipe.getForkCount(),
                recipe.getLikeCount() != null ? recipe.getLikeCount() : 0,
                verificationSummary.status() == InspectionStatus.COMPLETED,
                recipe.isDeleted()
        );
    }
}
