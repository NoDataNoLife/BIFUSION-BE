package com.nodatanolife.bifusion_be.domain.project.controller;

import com.nodatanolife.bifusion_be.domain.project.dto.request.ProjectInvitationRequest;
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectInvitationResponse;
import com.nodatanolife.bifusion_be.domain.project.service.ProjectInvitationService;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectInvitationService projectInvitationService;

    /**
     * 프로젝트 초대 생성 API.
     *
     * @param projectId 초대를 생성할 프로젝트 ID (path)
     * @param request   초대 대상 이메일을 담은 요청 DTO (이메일 형식 검증)
     * @param principal 현재 인증된 사용자 — 초대자(inviter)로 사용
     * @return 생성/재발송된 초대 정보
     */
    @PostMapping("/{projectId}/invitations")
    public CommonResponse<ProjectInvitationResponse> invite(
            @PathVariable Long projectId,
            @RequestBody @Valid ProjectInvitationRequest request,
            @AuthenticationPrincipal CustomOAuth2User principal
    ) {
        Long inviterUserId = principal.getUser().getUserId();

        ProjectInvitationResponse response =
                projectInvitationService.invite(projectId, inviterUserId, request.email());

        return CommonResponse.success(response, "초대 생성 완료!");
    }
}
