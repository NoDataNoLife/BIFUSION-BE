package com.nodatanolife.bifusion_be.domain.project.controller;

import com.nodatanolife.bifusion_be.domain.project.dto.request.ProjectCreateRequest;
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectCreateResponse;
import com.nodatanolife.bifusion_be.domain.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectCreateResponse> createProject(
            @RequestBody ProjectCreateRequest request,
            @AuthenticationPrincipal CustomOAuth2User oAuth2User
    ) {
        String email = oAuth2User.getUser().getEmail();

        ProjectCreateResponse response = projectService.createProject(request, email);

        return ResponseEntity.ok(response);
    }
}