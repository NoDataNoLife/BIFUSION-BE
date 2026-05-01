package com.nodatanolife.bifusion_be.domain.project.service;

import com.nodatanolife.bifusion_be.domain.project.dto.request.ProjectCreateRequest;
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectCreateResponse;
import com.nodatanolife.bifusion_be.domain.project.dto.request.MemberRoleUpdateRequest;
import com.nodatanolife.bifusion_be.domain.project.dto.response.MemberRoleUpdateResponse;
import com.nodatanolife.bifusion_be.domain.project.dto.request.ProjectVisibilityUpdateRequest;
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectVisibilityUpdateResponse;
import com.nodatanolife.bifusion_be.domain.user.entity.User;

public interface ProjectService {

    MemberRoleUpdateResponse updateMemberRole(Long projectId, Long targetUserId, MemberRoleUpdateRequest request, String email);

    void deleteByUser(User user);

    ProjectVisibilityUpdateResponse updateMyProjectVisibilities(Long userId, ProjectVisibilityUpdateRequest request);

    ProjectCreateResponse createProject(ProjectCreateRequest request, String email);
}