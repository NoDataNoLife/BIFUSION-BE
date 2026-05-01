package com.nodatanolife.bifusion_be.domain.project.controller;

import com.nodatanolife.bifusion_be.domain.project.dto.request.ProjectUpdateRequest;
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectUpdateResponse;
import jakarta.validation.Valid;
import com.nodatanolife.bifusion_be.domain.project.dto.request.ProjectCreateRequest;
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectCreateResponse;
import com.nodatanolife.bifusion_be.domain.project.service.ProjectService;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor

public class ProjectController {

    private final ProjectService projectService;

    /**
     * 프로젝트 생성 (기존 로직 완성)
     */
    @PostMapping
    public CommonResponse<ProjectCreateResponse> createProject(
            @Valid @RequestBody ProjectCreateRequest request,
            @AuthenticationPrincipal CustomOAuth2User oAuth2User
    ) {
        String email = oAuth2User.getUser().getEmail();
        ProjectCreateResponse response = projectService.createProject(request, email);

        return CommonResponse.success(response);
    }

    /**
     * 프로젝트 기본 정보 수정
     */
    @PutMapping("/{projectId}")
    public CommonResponse<ProjectUpdateResponse> updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectUpdateRequest request,
            @AuthenticationPrincipal CustomOAuth2User oAuth2User
    ) {
        String email = oAuth2User.getUser().getEmail();
        ProjectUpdateResponse response = projectService.updateProject(projectId, request, email);

        return CommonResponse.success(response, "프로젝트 정보가 수정되었습니다.");
    }
}