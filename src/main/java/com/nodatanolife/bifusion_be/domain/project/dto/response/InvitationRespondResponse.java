package com.nodatanolife.bifusion_be.domain.project.dto.response;

import com.nodatanolife.bifusion_be.domain.project.entity.InvitationStatus;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectInvitation;

public record InvitationRespondResponse(
        Long invitationId,
        InvitationStatus status
) {
    public static InvitationRespondResponse from(ProjectInvitation invitation) {
        return new InvitationRespondResponse(
                invitation.getProjectInvitationId(),
                invitation.getStatus()
        );
    }
}
