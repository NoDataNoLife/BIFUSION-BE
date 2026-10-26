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

    public sealed interface PublicActivityResponse permits
            PublicActivityResponse.RecipeItem,
            PublicActivityResponse.DatasetItem,
            PublicActivityResponse.QnaItem,
            PublicActivityResponse.RecruitItem,
            PublicActivityResponse.ProjectItem {

        LocalDateTime createdAt();

        // 1. RECIPE (쇼케이스)
        record RecipeItem(
                Long activityId,
                String type,
                String title,
                String description,
                @PresignedUrl String thumbnailUrl,
                Boolean isPublic,
                Integer likeCount,
                Long commentCount,
                Integer forkCount,
                LocalDateTime createdAt
        ) implements PublicActivityResponse {}

        // 2. DATASET
        record DatasetItem(
                Long activityId,
                String type,
                String title,
                String description,
                @PresignedUrl String thumbnailUrl,
                Boolean isPublic,
                Integer likeCount,
                Integer downloadCount,
                LocalDateTime createdAt
        ) implements PublicActivityResponse {}

        // 3. QNA
        record QnaItem(
                Long activityId,
                String type,
                String title,
                String description,
                Boolean isPublic,
                Integer commentCount,
                LocalDateTime createdAt
        ) implements PublicActivityResponse {}

        // 4. RECRUITMENT
        record RecruitItem(
                Long activityId,
                String type,
                String title,
                String description,
                Boolean isPublic,
                Integer commentCount,
                LocalDateTime createdAt
        ) implements PublicActivityResponse {}

        // 5. PROJECT
        record ProjectItem(
                Long activityId,
                String type,
                String title,
                String description,
                Boolean isPublic,
                String bannerImageUrl,
                String myRole,
                LocalDateTime createdAt
        ) implements PublicActivityResponse {}
    }
}
