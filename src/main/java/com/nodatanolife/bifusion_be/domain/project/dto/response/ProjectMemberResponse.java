package com.nodatanolife.bifusion_be.domain.project.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
public class ProjectMemberResponse {
    private Long userId;
    private String nickname;
    private String profileImageUrl;
}
