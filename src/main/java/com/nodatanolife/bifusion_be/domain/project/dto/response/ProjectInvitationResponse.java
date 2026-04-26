package com.nodatanolife.bifusion_be.domain.project.dto.response;

import com.nodatanolife.bifusion_be.domain.project.entity.InvitationStatus;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectInvitation;

import java.time.LocalDateTime;

public record ProjectInvitationResponse(
        Long projectInvitationId,
        String inviteeEmail,
        InvitationStatus status,
        LocalDateTime lastSentAt
) {
    public static ProjectInvitationResponse from(ProjectInvitation invitation) {
        return new ProjectInvitationResponse(
                invitation.getProjectInvitationId(),
                invitation.getInviteeEmail(),
                invitation.getStatus(),
                invitation.getLastSentAt()
        );
    }
}
