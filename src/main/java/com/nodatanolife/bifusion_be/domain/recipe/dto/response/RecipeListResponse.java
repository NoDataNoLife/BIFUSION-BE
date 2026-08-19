package com.nodatanolife.bifusion_be.domain.recipe.dto.response;

import com.nodatanolife.bifusion_be.domain.expert.dto.response.VerificationSummary;
import com.nodatanolife.bifusion_be.domain.expert.entity.VerificationStatus;
import com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

/**
 * 커뮤니티 레시피 목록 조회 응답 DTO.
 * RecipeRepository#searchRecipes가 이미 isPublic = true인 항목만 조회하므로
 * 이 응답에는 항상 공개 레시피만 담긴다.
 */
public record RecipeListResponse(
        int totalCount,
        List<RecipeSummary> content,
        PageInfo pageInfo
) {
    /**
     * 목록에 표시되는 레시피 요약 정보.
     */
    public record RecipeSummary(
            Long recipeId,
            String title,
            String bannerUrl,
            AuthorInfo author,
            int forkCount,
            int likeCount,
            boolean isExpertVerified
    ) {
        public static RecipeSummary from(Recipe recipe, VerificationSummary verificationSummary) {
            return new RecipeSummary(
                    recipe.getRecipeId(),
                    recipe.getTitle(),
                    recipe.getThumbnailUrl(),
                    AuthorInfo.from(recipe.getUser()),
                    recipe.getForkCount(),
                    recipe.getLikeCount() != null ? recipe.getLikeCount() : 0,
                    verificationSummary.status() == VerificationStatus.COMPLETED

            );
        }
    }

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

    /**
     * 페이지 정보. 전체 페이지 수 대신 hasNext(다음 페이지 존재 여부)만 제공한다.
     */
    public record PageInfo(int page, boolean hasNext) {
        public static PageInfo from(Page<?> page) {
            return new PageInfo(page.getNumber(), page.hasNext());
        }
    }

    public static RecipeListResponse of(Page<Recipe> page, Map<Long, VerificationSummary> verificationMap) {
        return new RecipeListResponse(
                (int) page.getTotalElements(),
                page.getContent().stream()
                        .map(recipe -> RecipeSummary.from(
                                recipe,
                                verificationMap.getOrDefault(recipe.getRecipeId(), VerificationSummary.none())
                        ))
                        .toList(),
                PageInfo.from(page)
        );
    }
}