package com.nodatanolife.bifusion_be.domain.project.dto.request;

import com.nodatanolife.bifusion_be.domain.project.entity.ProjectRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ProjectInvitationRequest(
        @NotBlank
        @Email
        String email,

        // 미지정 시 MEMBER로 초대
        ProjectRole role
) {
}
