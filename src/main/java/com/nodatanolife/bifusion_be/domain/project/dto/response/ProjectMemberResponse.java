package com.nodatanolife.bifusion_be.domain.project.dto.response;

import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProjectMemberResponse {
    private Long userId;
    private String nickname;
    @PresignedUrl
    private String profileImageUrl;
}
