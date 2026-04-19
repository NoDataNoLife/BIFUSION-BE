package com.nodatanolife.bifusion_be.domain.user.dto.response;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class BioUpdateResponse {
    private Long userId;
    private String bio;
    private LocalDateTime updatedAt;

    public static BioUpdateResponse from(User user) {
        return BioUpdateResponse.builder()
                .userId(user.getUserId())
                .bio(user.getBio())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}