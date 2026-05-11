package com.nodatanolife.bifusion_be.domain.project.controller;

import com.nodatanolife.bifusion_be.domain.project.dto.request.ProjectUpdateRequest;
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectUpdateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectInfoResponse;
import com.nodatanolife.bifusion_be.domain.project.dto.response.MyProjectsResponse;
import org.springframework.web.bind.annotation.*;


@Tag(name = "Project", description = "프로젝트 관리 API")
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "프로젝트 생성")
    @PostMapping
    public CommonResponse<ProjectCreateResponse> createProject(
            @Valid @RequestBody ProjectCreateRequest request,
            @AuthenticationPrincipal CustomOAuth2User oAuth2User
    ) {
        String email = oAuth2User.getUser().getEmail();
        ProjectCreateResponse response = projectService.createProject(request, email);

        return CommonResponse.success(response, "프로젝트 생성 성공!");
    }

    @Operation(summary = "프로젝트 기본 정보 수정")
    @PutMapping("/{projectId}")
    public CommonResponse<ProjectUpdateResponse> updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectUpdateRequest request,
            @AuthenticationPrincipal CustomOAuth2User oAuth2User
    ) {
        String email = oAuth2User.getUser().getEmail();
        ProjectUpdateResponse response = projectService.updateProject(projectId, request, email);

        return CommonResponse.success(response, "프로젝트 정보 수정 성공!");
    }

    @Operation(
            summary = "내가 참여한 프로젝트 목록 조회",
            description = "MANAGER / MEMBER 모두 포함합니다. 커서 기반 페이지네이션이며 기본 size는 4입니다."
    )
    @GetMapping("/me")
    public CommonResponse<MyProjectsResponse> getMyProjects(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "4") int size,
            @AuthenticationPrincipal CustomOAuth2User oAuth2User
    ) {
        String email = oAuth2User.getUser().getEmail();
        MyProjectsResponse response = projectService.getMyProjects(email, cursor, size);

        return CommonResponse.success(response, "참여한 프로젝트 목록 조회 성공!");
    }

    @Operation(summary = "프로젝트 기본 정보 조회")
    @GetMapping("/{projectId}")
    public CommonResponse<ProjectInfoResponse> getProjectInfo(
            @PathVariable Long projectId,
            @AuthenticationPrincipal CustomOAuth2User oAuth2User
    ) {
        String email = oAuth2User.getUser().getEmail();
        ProjectInfoResponse response = projectService.getProjectInfo(projectId, email);

        return CommonResponse.success(response, "프로젝트 상세 정보 조회 성공!");
    }

    @Operation(summary = "프로젝트 멤버 삭제")
    @DeleteMapping("/{projectId}/members/{userId}")
    public CommonResponse<Void> deleteMember(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomOAuth2User oAuth2User) {
        String email = oAuth2User.getUser().getEmail();
        projectService.deleteMember(projectId, userId, email);
        return CommonResponse.success(null, "프로젝트 멤버 삭제 성공!");

    }

    @Operation(summary = "프로젝트 멤버 역할 수정")
    @PutMapping("/{projectId}/members/{userId}")
    public CommonResponse<MemberRoleUpdateResponse> updateMemberRole(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @RequestBody MemberRoleUpdateRequest request,
            @AuthenticationPrincipal CustomOAuth2User oAuth2User) {
        String email = oAuth2User.getUser().getEmail();
        MemberRoleUpdateResponse response = projectService.updateMemberRole(projectId, userId, request, email);
        return CommonResponse.success(response, "프로젝트 멤버 역할 수정 성공!");
    }
}
