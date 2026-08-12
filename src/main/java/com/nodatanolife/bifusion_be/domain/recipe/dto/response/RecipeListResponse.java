package com.nodatanolife.bifusion_be.domain.recipe.dto.response;

import com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.domain.Page;

import java.util.List;

public record RecipeListResponse(
        int totalCount,
        List<RecipeSummary> content,
        PageInfo pageInfo
) {
    public record RecipeSummary(
            Long recipeId,
            String title,
            String bannerUrl,
            AuthorInfo author,
            int forkCount,
            int likeCount,
            boolean isExpertVerified
    ) {
        public static RecipeSummary from(Recipe recipe) {
            return new RecipeSummary(
                    recipe.getRecipeId(),
                    recipe.getTitle(),
                    recipe.getThumbnailUrl(),
                    AuthorInfo.from(recipe.getUser()),
                    recipe.getForkCount(),
                    recipe.getLikeCount() != null ? recipe.getLikeCount() : 0,
                    recipe.getIsExpertVerified()

            );
        }
    }

    public record AuthorInfo(Long userId, String nickname, String profileImageUrl) {
        public static AuthorInfo from(User author) {
            return new AuthorInfo(
                    author.getUserId(),
                    author.getNickname(),
                    author.getProfileImageUrl()
            );
        }
    }

    public record PageInfo(int page, boolean hasNext) {
        public static PageInfo from(Page<?> page) {
            return new PageInfo(page.getNumber(), page.hasNext());
        }
    }

    public static RecipeListResponse of(Page<Recipe> page) {
        return new RecipeListResponse(
                (int) page.getTotalElements(),
                page.getContent().stream().map(RecipeSummary::from).toList(),
                PageInfo.from(page)
        );
    }
}