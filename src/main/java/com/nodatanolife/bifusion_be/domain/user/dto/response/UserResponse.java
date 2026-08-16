package com.nodatanolife.bifusion_be.domain.user.dto.response;

import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertApplicationStatus;
import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
import com.nodatanolife.bifusion_be.domain.user.entity.SubscriptionTier;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import java.time.LocalDateTime;

public record UserResponse(
        Long userId,
        String name,
        String nickname,
        String email,
        String organization,
        String contact,
        @PresignedUrl String profileImageUrl,
        SubscriptionTier planType,
        ExpertApplicationStatus expertStatus,
        Integer points,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * User 엔티티 + 전문가 심사 상태 → UserResponse 변환
     * 컨트롤러에서 엔티티를 직접 반환하지 않고 DTO 로 변환해서 반환
     */
    public static UserResponse from(User user, ExpertApplicationStatus expertStatus) {
        return new UserResponse(
                user.getUserId(),
                user.getName(),
                user.getNickname(),
                user.getEmail(),
                user.getOrganization(),
                user.getContact(),
                user.getProfileImageUrl(),
                user.getPlanType(),
                expertStatus,
                user.getPoints() == null ? 0 : user.getPoints(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
