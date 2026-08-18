package com.nodatanolife.bifusion_be.domain.recipe.dto.response;

import com.nodatanolife.bifusion_be.domain.expert.dto.response.VerificationResultResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.VerificationSummary;
import com.nodatanolife.bifusion_be.domain.expert.entity.VerificationStatus;
import com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe;
import com.nodatanolife.bifusion_be.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public record RecipeDetailResponse(
        Long recipeId,
        String title,
        String description,
        String bannerUrl,
        boolean isExpertVerified,
        AuthorInfo author,
        LocalDateTime createdAt,
        double rating,
        int reviewCount,
        int forkCount,
        int viewCount,
        int downloadCount,
        Overview overview,
        Settings settings,
        VerificationStatus verificationStatus,
        VerificationResultResponse verificationResult
) {
    public record AuthorInfo(Long userId, String nickname, String profileImageUrl) {
        public static AuthorInfo from(User author) {
            return new AuthorInfo(
                    author.getUserId(),
                    author.getNickname(),
                    author.getProfileImageUrl()
            );
        }
    }

    public record Overview(String content, List<String> features, List<String> recommendations) {
        public static Overview from(Recipe recipe) {
            return new Overview(
                    recipe.getOverview(),
                    recipe.getKeyFeatures(),
                    recipe.getUseCases()
            );
        }
    }

    public record Settings(
            String model,
            int steps,
            String sampler,
            double cfgScale,
            String seed,
            String resolution,
            int batchSize
    ) {
        public static Settings from(Recipe recipe) {
            return new Settings(
                    recipe.getModel(),
                    recipe.getSteps(),
                    recipe.getSampler(),
                    recipe.getCfgScale().doubleValue(),
                    recipe.getSeed(),
                    recipe.getResolution(),
                    recipe.getBatchSize()
            );
        }
    }

    public static RecipeDetailResponse from(Recipe recipe, VerificationSummary verificationSummary) {
        return new RecipeDetailResponse(
                recipe.getRecipeId(),
                recipe.getTitle(),
                recipe.getShortDescription(),
                recipe.getThumbnailUrl(),
                verificationSummary.status() == VerificationStatus.COMPLETED,
                AuthorInfo.from(recipe.getUser()),
                recipe.getCreatedAt(),
                recipe.getRating().doubleValue(),
                0,
                recipe.getForkCount(),
                recipe.getViewCount(),
                recipe.getDownloadCount(),
                Overview.from(recipe),
                Settings.from(recipe),
                verificationSummary.status(),
                verificationSummary.result()
        );
    }
}
