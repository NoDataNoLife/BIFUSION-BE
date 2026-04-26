package com.nodatanolife.bifusion_be.domain.project.dto.request;

import com.nodatanolife.bifusion_be.domain.project.entity.InvitationAction;
import jakarta.validation.constraints.NotNull;

public record InvitationRespondRequest(
        @NotNull InvitationAction action
) {}
