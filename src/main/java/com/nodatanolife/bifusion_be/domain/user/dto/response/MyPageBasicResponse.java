package com.nodatanolife.bifusion_be.domain.user.dto.response;

import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
import com.nodatanolife.bifusion_be.domain.user.entity.SubscriptionTier;
import com.nodatanolife.bifusion_be.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public record MyPageBasicResponse(
        Long userId,
        String name,
        String nickname,
        String introduction,
        String location,
        String website,
        String createdAt,
        Boolean isExpert,
        SubscriptionTier planType,
        @PresignedUrl String profileImageUrl,
        List<PublicActivityResponse> publicActivities
) {
    public static MyPageBasicResponse from(User user, boolean isExpert, List<PublicActivityResponse> publicActivities) {
        return new MyPageBasicResponse(
                user.getUserId(),
                user.getName(),
                user.getNickname(),
                user.getBio(),
                user.getLocation(),
                user.getWebsiteUrl(),
                formatJoinedAt(user.getCreatedAt()),
                isExpert,
                user.getPlanType(),
                user.getProfileImageUrl(),
                publicActivities
        );
    }

    private static String formatJoinedAt(LocalDateTime createdAt) {
        if (createdAt == null) return null;
        return "%d년 %d월 가입".formatted(createdAt.getYear(), createdAt.getMonthValue());
    }

    public record PublicActivityResponse(
            ActivityType activityType,
            Long id,
            String title,
            String description,
            LocalDateTime createdAt,
            Boolean expertVerified,
            Integer likeCount,
            Integer commentCount,
            Integer downloadCount,
            Integer answerCount,
            String bannerImageUrl,
            String myRole
    ) {
        public enum ActivityType {
            SHOWCASE, DATASET, QNA, RECRUITMENT, PROJECT
        }

        private static PublicActivityResponse base(
                ActivityType type, Long id, String title, String description, LocalDateTime createdAt) {
            return new PublicActivityResponse(type, id, title, description, createdAt,
                    null, null, null, null, null, null, null);
        }

        public static PublicActivityResponse ofShowcase(com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe recipe, long commentCount) {
            var b = base(ActivityType.SHOWCASE, recipe.getRecipeId(), recipe.getTitle(),
                    recipe.getShortDescription(), recipe.getCreatedAt());
            return new PublicActivityResponse(b.activityType(), b.id(), b.title(), b.description(), b.createdAt(),
                    recipe.getIsExpertVerified(), recipe.getLikeCount(), (int) commentCount, null, null, null, null);
        }

        public static PublicActivityResponse ofDataset(com.nodatanolife.bifusion_be.domain.dataset.entity.Dataset dataset) {
            return new PublicActivityResponse(ActivityType.DATASET, dataset.getDatasetId(), dataset.getTitle(),
                    dataset.getDescription(), dataset.getCreatedAt(),
                    dataset.getIsExpertVerified(), dataset.getLikeCount(), null, dataset.getDownloadCount(),
                    null, null, null);
        }

        public static PublicActivityResponse ofQna(com.nodatanolife.bifusion_be.domain.qna.entity.ExpertQnA qna) {
            return new PublicActivityResponse(ActivityType.QNA, qna.getExpertQnAId(), qna.getTitle(),
                    qna.getContent(), qna.getCreatedAt(),
                    null, null, null, null, qna.getAnswerCount(), null, null);
        }

        public static PublicActivityResponse ofRecruitment(com.nodatanolife.bifusion_be.domain.recruitment.entity.Recruit recruit) {
            return new PublicActivityResponse(ActivityType.RECRUITMENT, recruit.getRecruitId(), recruit.getJobTitle(),
                    recruit.getDescription(), recruit.getCreatedAt(),
                    null, null, null, null, null, null, null);
        }

        public static PublicActivityResponse ofProject(com.nodatanolife.bifusion_be.domain.project.dto.response.MyProjectsResponse.Item item, LocalDateTime createdAt) {
            return new PublicActivityResponse(ActivityType.PROJECT, item.projectId(), item.title(),
                    null, createdAt,
                    null, null, null, null, null, item.bannerImageUrl(), item.role());
        }
    }
}
