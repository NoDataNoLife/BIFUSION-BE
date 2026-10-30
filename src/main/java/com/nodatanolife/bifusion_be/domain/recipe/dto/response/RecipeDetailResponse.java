package com.nodatanolife.bifusion_be.domain.recipe.dto.response;

import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionResultResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionSummary;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionTargetResponse;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionStatus;
import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
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
        @PresignedUrl String bannerUrl,
        boolean isExpertVerified,
        AuthorInfo author,
        LocalDateTime createdAt,
        double rating,
        int reviewCount,
        // 포크한 레시피는 원본의 포크 수
        int forkCount,
        int viewCount,
        int downloadCount,
        Overview overview,
        Settings settings,
        InspectionStatus inspectionStatus,
        InspectionResultResponse inspectionResult,
        // 검수 신청 버튼·취소용. 작성자 본인에게만 내려주고 그 외에는 null
        InspectionTargetResponse inspectionTarget,
        boolean isDeleted,
        // 포크한 레시피의 원본. 직접 작성한 레시피는 null
        ForkedFromInfo forkedFrom
) {
    /**
     * 작성자 정보.
     */
    public record AuthorInfo(Long userId, String nickname, @PresignedUrl String profileImageUrl) {
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
     * 생성 파라미터 설정값 영역. 레시피에 설정값이 없을 수 있어(엔티티 컬럼 nullable) 모든 값이 null일 수 있다.
     */
    public record Settings(
            String model,
            Integer steps,
            String sampler,
            Double cfgScale,
            String seed,
            String resolution,
            Integer batchSize
    ) {
        public static Settings from(Recipe recipe) {
            return new Settings(
                    recipe.getModel(),
                    recipe.getSteps(),
                    recipe.getSampler(),
                    recipe.getCfgScale() != null ? recipe.getCfgScale().doubleValue() : null,
                    recipe.getSeed(),
                    recipe.getResolution(),
                    recipe.getBatchSize()
            );
        }
    }

    /**
     * @param reviewCount 최상위 리뷰 + 대댓글 합산 (리뷰 목록 API의 totalElements는 최상위 리뷰 수라 다르다)
     */
    public static RecipeDetailResponse from(Recipe recipe, InspectionSummary verificationSummary, long reviewCount,
                                            InspectionTargetResponse inspectionTarget) {
        return new RecipeDetailResponse(
                recipe.getRecipeId(),
                recipe.getTitle(),
                recipe.getShortDescription(),
                recipe.getThumbnailUrl(),
                verificationSummary.status() == InspectionStatus.COMPLETED,
                AuthorInfo.from(recipe.getUser()),
                recipe.getCreatedAt(),
                // 카운트 컬럼이 비어 있는 예전 데이터가 있어 목록 응답과 같이 0으로 본다
                recipe.getRating() != null ? recipe.getRating().doubleValue() : 0.0,
                (int) reviewCount,
                recipe.getDisplayForkCount(),
                recipe.getViewCount() != null ? recipe.getViewCount() : 0,
                recipe.getDownloadCount() != null ? recipe.getDownloadCount() : 0,
                Overview.from(recipe),
                Settings.from(recipe),
                verificationSummary.status(),
                verificationSummary.result(),
                inspectionTarget,
                recipe.isDeleted(),
                ForkedFromInfo.of(recipe)
        );
    }
}
