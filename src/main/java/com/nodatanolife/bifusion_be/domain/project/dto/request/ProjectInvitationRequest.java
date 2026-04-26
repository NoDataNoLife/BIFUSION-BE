package com.nodatanolife.bifusion_be.domain.project.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ProjectInvitationRequest(
        @NotBlank
        @Email
        String email
) {
}