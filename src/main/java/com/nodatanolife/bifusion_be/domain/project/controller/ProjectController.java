package com.nodatanolife.bifusion_be.domain.project.controller;

import com.nodatanolife.bifusion_be.domain.project.dto.request.ProjectUpdateRequest;
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectUpdateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.nodatanolife.bifusion_be.domain.project.dto.request.ProjectCreateRequest;
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectCreateResponse;
import com.nodatanolife.bifusion_be.domain.project.dto.request.*;
import com.nodatanolife.bifusion_be.domain.project.dto.response.*;
import com.nodatanolife.bifusion_be.domain.project.service.ProjectService;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Project", description = "프로젝트 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects")
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
            description = "MANAGER / MEMBER 모두 포함합니다. 커서 기반 페이지네이션이며 기본 size는 4입니다. "
                    + "isPublic 파라미터를 넘기면 DB 단계에서 해당 공개 여부(사용자별 설정)로 필터링한 뒤 페이징합니다. "
                    + "예) isPublic=true 이면 프로필에 공개된 프로젝트만 조회합니다."
    )
    @GetMapping("/me")
    public CommonResponse<MyProjectsResponse> getMyProjects(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "4") int size,
            @RequestParam(required = false) Boolean isPublic,
            @AuthenticationPrincipal CustomOAuth2User oAuth2User
    ) {
        String email = oAuth2User.getUser().getEmail();
        MyProjectsResponse response = projectService.getMyProjects(email, cursor, size, isPublic);

        return CommonResponse.success(response, "참여한 프로젝트 목록 조회 성공!");
    }

    @Operation(
            summary = "프로젝트 삭제",
            description = """
                    프로젝트를 삭제합니다. 프로젝트 관리자(MANAGER)만 삭제할 수 있습니다.
                    삭제한 프로젝트는 목록·상세에서 더 이상 조회되지 않습니다 (상세 등 프로젝트 API는 404 또는 403).

                    - 멤버·즐겨찾기·초대는 함께 삭제됩니다. 받은 초대 알림은 남지만 초대 정보(invitation)는 null로 내려옵니다.
                    - 증강 데이터 검수 신청 중 전문가가 아직 시작하지 않은 것은 취소되고 포인트가 환불됩니다.
                    - 배너, 작업 입력·결과 파일, 증강 결과, 학습 모델 파일이 삭제됩니다 (작업 파일은 저장 용량 집계에서 빠짐).
                      데이터셋에 첨부한 파일은 남습니다.
                    - 없거나 이미 삭제된 프로젝트면 404(P001), 관리자가 아니면 403(P002)
                    - 진행 중(PENDING/RUNNING)인 작업이 있으면 409(P011). 작업이 끝나거나 취소한 뒤 다시 시도합니다.
                    - 전문가가 검수 중인 증강 데이터가 있으면 409(IS012)
                    """
    )
    @DeleteMapping("/{projectId}")
    public CommonResponse<Void> deleteProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal CustomOAuth2User oAuth2User
    ) {
        projectService.deleteProject(projectId, oAuth2User.getUser().getUserId());
        return CommonResponse.success(null, "프로젝트 삭제 성공!");
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

    @Operation(
            summary = "관리/참여 중인 프로젝트 목록 조회",
            description = "사용자가 관리자(MANAGER) 또는 일반 멤버(MEMBER)로 관리/참여 중인 모든 프로젝트 리스트를 조회합니다."
    )
    @GetMapping
    public CommonResponse<MyProjectDashboardResponse> getMyProjects(@AuthenticationPrincipal CustomOAuth2User principal) {
        MyProjectDashboardResponse response = projectService.getMyProjectDashboard(principal.getUser().getUserId());
        return CommonResponse.success(response, "프로젝트 목록 조회 성공!");
    }

    @Operation(summary = "프로젝트 배너 이미지 등록/변경", description = "file이 없으면 배너 이미지를 삭제합니다.")
    @PatchMapping(value = "/{projectId}/banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResponse<ProjectBannerUpdateResponse> updateBannerImage(
            @PathVariable Long projectId,
            @RequestPart(required = false) MultipartFile file,
            @AuthenticationPrincipal CustomOAuth2User oAuth2User
    ) {
        String email = oAuth2User.getUser().getEmail();
        ProjectBannerUpdateResponse response = projectService.updateBannerImage(projectId, file, email);
        return CommonResponse.success(response, "배너 이미지 수정 성공!");
    }
}
