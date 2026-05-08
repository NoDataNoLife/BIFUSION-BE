package com.nodatanolife.bifusion_be.domain.project.service;

import com.nodatanolife.bifusion_be.domain.project.dto.request.ProjectCreateRequest;
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectCreateResponse;
import com.nodatanolife.bifusion_be.domain.project.dto.request.ProjectVisibilityUpdateRequest;
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectVisibilityUpdateResponse;
import com.nodatanolife.bifusion_be.domain.user.entity.User;

public interface ProjectService {
    void deleteByUser(User user);
    ProjectVisibilityUpdateResponse updateMyProjectVisibilities(Long userId, ProjectVisibilityUpdateRequest request);

    ProjectCreateResponse createProject(ProjectCreateRequest request, String email);
}