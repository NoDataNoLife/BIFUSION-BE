package com.nodatanolife.bifusion_be.domain.project.dto.request;

import com.nodatanolife.bifusion_be.domain.project.entity.ProjectRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectInvitationRequest(
        @NotBlank
        @Email
        @Size(max = 255, message = "이메일은 255자 이하로 입력해 주세요.")
        String email,

        // 미지정 시 MEMBER로 초대
        ProjectRole role
) {
}
