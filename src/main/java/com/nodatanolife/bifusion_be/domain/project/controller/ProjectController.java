package com.nodatanolife.bifusion_be.domain.project.controller;

import jakarta.validation.Valid;
import com.nodatanolife.bifusion_be.domain.project.dto.request.ProjectCreateRequest;
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectCreateResponse;
import com.nodatanolife.bifusion_be.domain.project.service.ProjectService;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import com.nodatanolife.bifusion_be.domain.project.dto.request.MemberRoleUpdateRequest;
import com.nodatanolife.bifusion_be.domain.project.dto.response.MemberRoleUpdateResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public CommonResponse<ProjectCreateResponse> createProject(
            @Valid @RequestBody ProjectCreateRequest request,
            @AuthenticationPrincipal CustomOAuth2User oAuth2User
    ) {
        String email = oAuth2User.getUser().getEmail();

        ProjectCreateResponse response = projectService.createProject(request, email);

        return CommonResponse.success(response);
    }

    @PutMapping("/{projectId}/members/{userId}")
    public CommonResponse<MemberRoleUpdateResponse> updateMemberRole(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @RequestBody MemberRoleUpdateRequest request,
            @AuthenticationPrincipal CustomOAuth2User oAuth2User) {
        String email = oAuth2User.getUser().getEmail();
        MemberRoleUpdateResponse response = projectService.updateMemberRole(projectId, userId, request, email);
        return CommonResponse.success(response, "프로젝트 멤버 역할이 성공적으로 수정되었습니다.");
    }
}