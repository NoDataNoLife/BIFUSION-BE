package com.nodatanolife.bifusion_be.domain.user.dto.response;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class WebsiteUpdateResponse {
    private Long userId;
    private String websiteUrl;
    private LocalDateTime updatedAt;

    public static WebsiteUpdateResponse from(User user) {
        return WebsiteUpdateResponse.builder()
                .userId(user.getUserId())
                .websiteUrl(user.getWebsiteUrl())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}