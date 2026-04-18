package com.nodatanolife.bifusion_be.domain.user.dto.response;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import java.time.LocalDateTime;

public record UserResponse(
        Long userId,
        String name,
        String nickname,
        String email,
        String organization,
        String contact,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * User 엔티티 → UserResponse 변환
     * 컨트롤러에서 엔티티를 직접 반환하지 않고 DTO 로 변환해서 반환
     */
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getName(),
                user.getNickname(),
                user.getEmail(),
                user.getOrganization(),
                user.getContact(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}