package com.nodatanolife.bifusion_be.domain.project.controller;

import com.nodatanolife.bifusion_be.domain.project.dto.request.InvitationRespondRequest;
import com.nodatanolife.bifusion_be.domain.project.dto.request.ProjectInvitationRequest;
import com.nodatanolife.bifusion_be.domain.project.dto.response.InvitationRespondResponse;
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectInvitationResponse;
import com.nodatanolife.bifusion_be.domain.project.service.ProjectInvitationService;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Project Invitation", description = "프로젝트 초대 API")
@RestController
@RequestMapping("/api/v1/projects/{projectId}/invitations")
@RequiredArgsConstructor
public class ProjectInvitationController {

    private final ProjectInvitationService projectInvitationService;

    @Operation(
            summary = "프로젝트 초대 생성",
            description = "이메일로 프로젝트 초대를 생성하거나 재발송합니다. 인증된 사용자가 초대자(inviter)로 사용됩니다. "
                    + "role 미지정 시 MEMBER로 초대되며, MANAGER 초대는 MANAGER만 가능합니다. 재발송 시 요청한 role로 갱신됩니다."
    )
    @PostMapping
    public CommonResponse<ProjectInvitationResponse> invite(
            @PathVariable Long projectId,
            @RequestBody @Valid ProjectInvitationRequest request,
            @AuthenticationPrincipal CustomOAuth2User principal
    ) {
        Long inviterUserId = principal.getUser().getUserId();

        ProjectInvitationResponse response =
                projectInvitationService.invite(projectId, inviterUserId, request.email(), request.role());

        return CommonResponse.success(response, "초대 생성 성공!");
    }

    @Operation(
            summary = "프로젝트 초대 수락/거절",
            description = "초대받은 사용자 본인만 응답할 수 있으며, ACCEPT 또는 REJECT 액션으로 status가 갱신됩니다."
    )
    @PatchMapping("/{invitationId}")
    public CommonResponse<InvitationRespondResponse> respond(
            @PathVariable Long projectId,
            @PathVariable Long invitationId,
            @RequestBody @Valid InvitationRespondRequest request,
            @AuthenticationPrincipal CustomOAuth2User principal
    ) {
        Long userId = principal.getUser().getUserId();
        InvitationRespondResponse response =
                projectInvitationService.respond(projectId, invitationId, userId, request.action());
        return CommonResponse.success(response, "프로젝트 초대 응답 성공!");
    }
}
