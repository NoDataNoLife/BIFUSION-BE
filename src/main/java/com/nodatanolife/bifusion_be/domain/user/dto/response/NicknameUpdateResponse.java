package com.nodatanolife.bifusion_be.domain.user.dto.response;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class NicknameUpdateResponse {
    private Long userId;
    private String nickname;
    private LocalDateTime updatedAt;

    public static NicknameUpdateResponse from(User user) {
        return NicknameUpdateResponse.builder()
                .userId(user.getUserId())      // getId() → getUserId()
                .nickname(user.getNickname())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}