package com.nodatanolife.bifusion_be.domain.project.service;

import com.nodatanolife.bifusion_be.domain.project.dto.request.ProjectCreateRequest;
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectCreateResponse;
import com.nodatanolife.bifusion_be.domain.project.dto.request.MemberRoleUpdateRequest;
import com.nodatanolife.bifusion_be.domain.project.dto.response.MemberRoleUpdateResponse;
import com.nodatanolife.bifusion_be.domain.project.dto.request.ProjectUpdateRequest;
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectUpdateResponse;
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectInfoResponse;
import com.nodatanolife.bifusion_be.domain.project.dto.response.MyProjectsResponse;
import com.nodatanolife.bifusion_be.domain.project.dto.request.ProjectVisibilityUpdateRequest;
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectVisibilityUpdateResponse;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.project.dto.response.MyProjectDashboardResponse;
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectBannerUpdateResponse;
import org.springframework.web.multipart.MultipartFile;


public interface ProjectService {

    MemberRoleUpdateResponse updateMemberRole(Long projectId, Long targetUserId, MemberRoleUpdateRequest request, String email);

    void deleteMember(Long projectId, Long targetUserId, String email);

    ProjectInfoResponse getProjectInfo(Long projectId, String email);

    MyProjectsResponse getMyProjects(String email, String cursor, int size, Boolean isPublic);

    void deleteByUser(User user);

    ProjectVisibilityUpdateResponse updateMyProjectVisibilities(Long userId, ProjectVisibilityUpdateRequest request);

    ProjectCreateResponse createProject(ProjectCreateRequest request, String email);

    ProjectUpdateResponse updateProject(Long projectId, ProjectUpdateRequest request, String email);

    MyProjectDashboardResponse getMyProjectDashboard(Long userId);

    ProjectBannerUpdateResponse updateBannerImage(Long projectId, MultipartFile file, String email);


}

