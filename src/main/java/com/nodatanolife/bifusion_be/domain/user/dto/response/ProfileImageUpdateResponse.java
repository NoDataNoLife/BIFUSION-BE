package com.nodatanolife.bifusion_be.domain.user.dto.response;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProfileImageUpdateResponse {
    private String profileImageUrl;

    public static ProfileImageUpdateResponse from(User user) {
        return ProfileImageUpdateResponse.builder()
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }
}