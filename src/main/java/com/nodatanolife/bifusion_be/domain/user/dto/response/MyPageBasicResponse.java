package com.nodatanolife.bifusion_be.domain.user.dto.response;

import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
import com.nodatanolife.bifusion_be.domain.user.entity.SubscriptionTier;
import com.nodatanolife.bifusion_be.domain.user.entity.User;

import java.time.LocalDateTime;

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
        @PresignedUrl String profileImageUrl
) {
    public static MyPageBasicResponse from(User user) {
        return new MyPageBasicResponse(
                user.getUserId(),
                user.getName(),
                user.getNickname(),
                user.getBio(),
                user.getLocation(),
                user.getWebsiteUrl(),
                formatJoinedAt(user.getCreatedAt()),
                user.getIsExpert(),
                user.getPlanType(),
                user.getProfileImageUrl()
        );
    }

    private static String formatJoinedAt(LocalDateTime createdAt) {
        if (createdAt == null) return null;
        return "%d년 %d월 가입".formatted(createdAt.getYear(), createdAt.getMonthValue());
    }
}
