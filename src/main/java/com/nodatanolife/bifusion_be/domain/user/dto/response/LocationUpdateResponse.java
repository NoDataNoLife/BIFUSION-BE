package com.nodatanolife.bifusion_be.domain.user.dto.response;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class LocationUpdateResponse {
    private Long userId;
    private String location;
    private LocalDateTime updatedAt;

    public static LocationUpdateResponse from(User user) {
        return LocationUpdateResponse.builder()
                .userId(user.getUserId())
                .location(user.getLocation())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}