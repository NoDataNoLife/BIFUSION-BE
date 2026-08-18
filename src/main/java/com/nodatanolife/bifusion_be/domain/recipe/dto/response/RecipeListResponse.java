package com.nodatanolife.bifusion_be.domain.recipe.dto.response;

import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionSummary;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionStatus;
import com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

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
        public static RecipeSummary from(Recipe recipe, InspectionSummary verificationSummary) {
            return new RecipeSummary(
                    recipe.getRecipeId(),
                    recipe.getTitle(),
                    recipe.getThumbnailUrl(),
                    AuthorInfo.from(recipe.getUser()),
                    recipe.getForkCount(),
                    recipe.getLikeCount() != null ? recipe.getLikeCount() : 0,
                    verificationSummary.status() == InspectionStatus.COMPLETED

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

    public static RecipeListResponse of(Page<Recipe> page, Map<Long, InspectionSummary> verificationMap) {
        return new RecipeListResponse(
                (int) page.getTotalElements(),
                page.getContent().stream()
                        .map(recipe -> RecipeSummary.from(
                                recipe,
                                verificationMap.getOrDefault(recipe.getRecipeId(), InspectionSummary.none())
                        ))
                        .toList(),
                PageInfo.from(page)
        );
    }
}