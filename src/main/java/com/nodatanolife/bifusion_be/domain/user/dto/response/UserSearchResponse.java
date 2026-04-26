package com.nodatanolife.bifusion_be.domain.user.dto.response;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserSearchResponse {

    private boolean exists;
    private Long userId;
    private String email;
    private String nickname;
    private String profileImage;

    public static UserSearchResponse found(User user) {
        return UserSearchResponse.builder()
                .exists(true)
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImage(user.getProfileImageUrl())
                .build();
    }

    public static UserSearchResponse notFound(String email) {
        return UserSearchResponse.builder()
                .exists(false)
                .email(email)
                .build();
    }
}