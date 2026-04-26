package com.nodatanolife.bifusion_be.domain.project.service;

import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectInvitationResponse;

public interface ProjectInvitationService {

    ProjectInvitationResponse invite(Long projectId, Long inviterUserId, String email);
}
