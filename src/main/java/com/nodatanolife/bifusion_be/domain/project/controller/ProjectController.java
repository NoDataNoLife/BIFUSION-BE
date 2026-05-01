package com.nodatanolife.bifusion_be.domain.project.controller;

import jakarta.validation.Valid;
import com.nodatanolife.bifusion_be.domain.project.dto.request.ProjectCreateRequest;
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectCreateResponse;
import com.nodatanolife.bifusion_be.domain.project.service.ProjectService;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectInfoResponse;
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

        return CommonResponse.success(response, "프로젝트 생성 성공!");
    }

    @GetMapping("/{projectId}")
    public CommonResponse<ProjectInfoResponse> getProjectInfo(
            @PathVariable Long projectId,
            @AuthenticationPrincipal CustomOAuth2User oAuth2User) {
        String email = oAuth2User.getUser().getEmail();
        ProjectInfoResponse response = projectService.getProjectInfo(projectId, email);
        return CommonResponse.success(response, "프로젝트 상세 정보 조회 성공!");   }
}