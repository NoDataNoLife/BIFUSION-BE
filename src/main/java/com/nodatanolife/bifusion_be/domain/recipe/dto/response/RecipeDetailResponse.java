package com.nodatanolife.bifusion_be.domain.recipe.dto.response;

import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionResultResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionSummary;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionStatus;
import com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe;
import com.nodatanolife.bifusion_be.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 레시피 상세 조회 응답 DTO.
 * 비공개 레시피인 경우 서비스단(RecipeServiceImpl#getRecipeDetail)에서
 * 작성자 본인이 아니면 이 응답을 만들기 전에 이미 예외를 던지므로,
 * 여기까지 도달했다면 "공개 레시피" 또는 "본인 소유 비공개/포크 레시피"만 담긴다.
 */
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
        InspectionStatus inspectionStatus,
        InspectionResultResponse inspectionResult,
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

    /**
     * 레시피 본문 설명 영역 (개요/특징/추천 용도).
     */
    public record Overview(String content, List<String> features, List<String> recommendations) {
        public static Overview from(Recipe recipe) {
            return new Overview(
                    recipe.getOverview(),
                    recipe.getKeyFeatures(),
                    recipe.getUseCases()
            );
        }
    }

    /**
     * 생성 파라미터 설정값 영역.
     */
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

    public static RecipeDetailResponse from(Recipe recipe, InspectionSummary verificationSummary) {
        return new RecipeDetailResponse(
                recipe.getRecipeId(),
                recipe.getTitle(),
                recipe.getShortDescription(),
                recipe.getThumbnailUrl(),
                verificationSummary.status() == InspectionStatus.COMPLETED,
                AuthorInfo.from(recipe.getUser()),
                recipe.getCreatedAt(),
                recipe.getRating().doubleValue(),
                // TODO: 실제 댓글 수 집계 미연동 상태 (항상 0으로 고정)
                0,
                recipe.getForkCount(),
                recipe.getViewCount(),
                recipe.getDownloadCount(),
                Overview.from(recipe),
                Settings.from(recipe),
                verificationSummary.status(),
                verificationSummary.result(),
                recipe.isDeleted()
        );
    }
}
