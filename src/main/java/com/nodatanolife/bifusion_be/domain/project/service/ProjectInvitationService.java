package com.nodatanolife.bifusion_be.domain.project.service;

import com.nodatanolife.bifusion_be.domain.project.dto.response.InvitationRespondResponse;
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectInvitationResponse;
import com.nodatanolife.bifusion_be.domain.project.entity.InvitationAction;
import com.nodatanolife.bifusion_be.domain.user.entity.User;

public interface ProjectInvitationService {

    ProjectInvitationResponse invite(Long projectId, Long inviterUserId, String email);

    InvitationRespondResponse respond(Long projectId, Long invitationId, Long userId, InvitationAction action);

    void linkPendingInvitationsTo(User user);
}
