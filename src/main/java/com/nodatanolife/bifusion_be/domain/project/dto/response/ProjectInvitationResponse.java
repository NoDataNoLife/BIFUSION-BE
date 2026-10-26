package com.nodatanolife.bifusion_be.domain.project.dto.response;

import com.nodatanolife.bifusion_be.domain.project.entity.InvitationStatus;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectInvitation;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectRole;

import java.time.LocalDateTime;

public record ProjectInvitationResponse(
        Long projectInvitationId,
        String inviteeEmail,
        ProjectRole role,
        InvitationStatus status,
        LocalDateTime lastSentAt
) {
    public static ProjectInvitationResponse from(ProjectInvitation invitation) {
        return new ProjectInvitationResponse(
                invitation.getProjectInvitationId(),
                invitation.getInviteeEmail(),
                invitation.getRole(),
                invitation.getStatus(),
                invitation.getLastSentAt()
        );
    }
}
