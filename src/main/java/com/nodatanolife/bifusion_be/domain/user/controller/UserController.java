package com.nodatanolife.bifusion_be.domain.user.controller;

import com.nodatanolife.bifusion_be.domain.user.dto.request.BioUpdateRequest;
import com.nodatanolife.bifusion_be.domain.user.dto.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.multipart.MultipartFile;
import com.nodatanolife.bifusion_be.domain.user.dto.request.WebsiteUpdateRequest;
import com.nodatanolife.bifusion_be.domain.user.dto.request.LocationUpdateRequest;
import com.nodatanolife.bifusion_be.domain.user.dto.request.NicknameUpdateRequest;
import com.nodatanolife.bifusion_be.domain.user.dto.request.OnboardingRequest;
import com.nodatanolife.bifusion_be.domain.project.dto.request.ProjectVisibilityUpdateRequest;
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectVisibilityUpdateResponse;
import com.nodatanolife.bifusion_be.domain.project.service.ProjectService;
import com.nodatanolife.bifusion_be.domain.user.dto.request.ActivityVisibilityUpdateRequest;
import com.nodatanolife.bifusion_be.domain.user.dto.response.ActivityVisibilityUpdateResponse;
import com.nodatanolife.bifusion_be.domain.user.dto.response.MyActivityListResponse;
import com.nodatanolife.bifusion_be.domain.user.service.MyActivityService;
import com.nodatanolife.bifusion_be.domain.user.service.MyActivityVisibilityService;
import com.nodatanolife.bifusion_be.domain.user.service.ResourceUsageService;
import com.nodatanolife.bifusion_be.domain.user.service.UserService;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import com.nodatanolife.bifusion_be.global.security.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "유저 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class UserController {

    private final UserService userService;
    private final ProjectService projectService;
    private final MyActivityVisibilityService myActivityVisibilityService;
    private final MyActivityService myActivityService;
    private final ResourceUsageService resourceUsageService;
    private final CookieUtil cookieUtil;

    @Operation(
            summary = "내 정보 조회",
            description = """
                    JWT 쿠키로 인증된 본인의 계정 정보를 반환합니다.
                    이메일, 연락처 등 민감 정보를 포함하므로 본인만 조회 가능합니다.
                    앱 진입 시 호출해 로그인 상태와 본인 식별 정보를 확인하는 용도로 사용합니다.
                    다른 유저의 공개 프로필이 필요하면 GET /users/{userId} 를 사용하세요."""
    )
    @GetMapping("/users/me")
    public CommonResponse<UserResponse> getMe(
            @AuthenticationPrincipal CustomOAuth2User principal) {
        UserResponse response = userService.getMe(principal.getUser().getUserId());
        return CommonResponse.success(response, "내 정보 조회 성공!");
    }

    @Operation(
            summary = "내 리소스 사용량 조회",
            description = """
                    대시보드의 스토리지·월간 증강 카드에 쓰는 사용량을 조회합니다.

                    - storageUsedBytes: 내가 소유한 파일(데이터셋 업로드·작업 입력 이미지·증강 결과·학습 모델)의 크기 합.
                      1시간 뒤 정리되는 임시 업로드와 삭제된 데이터셋의 파일은 제외합니다.
                    - storageLimitBytes: 저장 용량 한도. 아직 정해지지 않아 null
                    - storageUnmeasuredFileCount: 크기 정보가 없어 합계에서 빠진 파일 수
                    - monthlyAugmentations: 이번 달(periodStart ~ periodEnd) 만든 증강 작업 수. 실패·취소는 제외, 진행 중은 포함
                    - monthlyAugmentationLimit: 횟수 제한이 없어 null (작업 비용은 토큰으로 차감)
                    - periodStart/periodEnd: 집계 기간 (timezone 기준 이번 달 1일 0시 ~ 다음 달 1일 0시, 끝은 미포함)

                    사용량 값이 null이면 집계에 실패한 것이고, 0이면 실제 사용량이 없는 것입니다."""
    )
    @GetMapping("/users/me/resource-usage")
    public CommonResponse<ResourceUsageResponse> getMyResourceUsage(
            @AuthenticationPrincipal CustomOAuth2User principal) {
        ResourceUsageResponse response = resourceUsageService.getResourceUsage(principal.getUser().getUserId());
        return CommonResponse.success(response, "리소스 사용량 조회 성공!");
    }

    @Operation(
            summary = "마이페이지 기본 정보 조회",
            description = """
                    임의의 유저의 공개 프로필 정보를 반환합니다.
                    다른 유저의 마이페이지 방문 시 사용하며, 이메일/연락처 같은 민감 정보는 포함되지 않습니다.
                    본인 계정 정보가 필요하면 GET /users/me 를 사용하세요."""
    )
    @GetMapping("/users/{userId}")
    public CommonResponse<MyPageBasicResponse> getMyPageBasic(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int size) {
        MyPageBasicResponse response = userService.getMyPageBasic(userId, size);
        return CommonResponse.success(response, "마이페이지 조회 성공!");
    }

    @Operation(
            summary = "닉네임 수정",
            description = "인증된 유저의 닉네임을 변경합니다."
    )
    @PutMapping("/profile/nickname")
    public CommonResponse<NicknameUpdateResponse> updateNickname(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestBody @Valid NicknameUpdateRequest request) {

        NicknameUpdateResponse response = userService.updateNickname(
                principal.getUser().getUserId(), request.getNickname());
        return CommonResponse.success(response, "닉네임 수정 성공!");
    }

    @Operation(summary = "소개글 수정")
    @PutMapping("/profile/introduction")
    public CommonResponse<BioUpdateResponse> updateBio(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestBody @Valid BioUpdateRequest request) {
        BioUpdateResponse response = userService.updateBio(
                principal.getUser().getUserId(), request.getBio());
        return CommonResponse.success(response, "소개글 수정 성공!");
    }

    @Operation(
            summary = "위치 수정",
            description = "인증된 유저의 위치를 변경합니다."
    )
    @PutMapping("/profile/location")
    public CommonResponse<LocationUpdateResponse> updateLocation(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestBody @Valid LocationUpdateRequest request) {
        LocationUpdateResponse response = userService.updateLocation(
                principal.getUser().getUserId(), request.getLocation());
        return CommonResponse.success(response, "위치 수정 완료!");
    }

    @Operation(
            summary = "웹사이트 수정",
            description = "인증된 유저의 웹사이트를 변경합니다."
    )
    @PutMapping("/profile/website")
    public CommonResponse<WebsiteUpdateResponse> updateWebsiteUrl(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestBody @Valid WebsiteUpdateRequest request) {
        WebsiteUpdateResponse response = userService.updateWebsiteUrl(
                principal.getUser().getUserId(), request.getWebsiteUrl());
        return CommonResponse.success(response, "웹사이트 수정 성공!");
    }

    @Operation(summary = "프로필 사진 수정")
    @PutMapping("/profile/image")
    public CommonResponse<ProfileImageUpdateResponse> updateProfileImage(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestPart MultipartFile image) {
        ProfileImageUpdateResponse response = userService.updateProfileImage(
                principal.getUser().getUserId(), image);
        return CommonResponse.success(response, "프로필 사진 수정 성공!");
    }

    @Operation(
            summary = "온보딩 완료 처리",
            description = "최초 로그인 후 추가 정보 입력 시 호출합니다."
    )
    @PostMapping("/onboarding")
    public CommonResponse<UserResponse> onboarding(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestBody @Valid OnboardingRequest request) {

        UserResponse response = userService.onboard(
                principal.getUser().getUserId(), request);

        return CommonResponse.success(response, "회원가입 성공!");
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "인증 정보 삭제 → 개인 기록 삭제 → 공개 콘텐츠 익명화 → User 삭제 순으로 처리합니다."
    )
    @DeleteMapping("/users/me")
    public CommonResponse<Void> withdraw(
            @AuthenticationPrincipal CustomOAuth2User principal,
            HttpServletResponse response) {

        userService.withdraw(principal.getUser().getUserId());

        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieUtil.expireCookie("access_token").toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieUtil.expireCookie("refresh_token").toString());

        return CommonResponse.success(null, "회원 탈퇴 성공!");
    }

    @Operation(
            summary = "이메일로 유저 검색",
            description = "이메일 기반으로 유저를 조회합니다. 미가입 이메일인 경우 exists: false 로 응답합니다."
    )
    @GetMapping("/users/search")
    public CommonResponse<UserSearchResponse> searchByEmail(
            @RequestParam @Email @NotBlank String email
    ) {
        UserSearchResponse response = userService.searchByEmail(email);
        return CommonResponse.success(response, "유저 이메일 검색 성공!");
    }


    @Operation(
            summary = "내 프로젝트 공개 여부 일괄 변경",
            description = "마이페이지에서 내가 참여한 프로젝트들의 공개 여부를 일괄 변경합니다. 비공개 프로젝트는 공개로 변경할 수 없습니다."
    )
    @PutMapping("/users/me/projects/visibility")
    public CommonResponse<ProjectVisibilityUpdateResponse> updateMyProjectVisibilities(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestBody @Valid ProjectVisibilityUpdateRequest request) {

        ProjectVisibilityUpdateResponse response = projectService.updateMyProjectVisibilities(
                principal.getUser().getUserId(), request);

        return CommonResponse.success(response, "프로젝트 공개여부 변경 성공!");
    }

    @Operation(
            summary = "마이페이지 활동 공개 여부 일괄 변경",
            description = "Recipe / Dataset / Q&A / Recruitment 항목의 공개 여부를 한 번에 처리합니다. " +
                    "콘텐츠 자체가 비공개면 마이페이지에도 강제 비공개로 저장되며, 본인 소유가 아닌 항목이 포함되면 전체 롤백됩니다.")
    @PatchMapping("/users/me/activities/visibility")
    public CommonResponse<ActivityVisibilityUpdateResponse> updateMyActivityVisibilities(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestBody @Valid ActivityVisibilityUpdateRequest request) {

        ActivityVisibilityUpdateResponse response = myActivityVisibilityService.updateVisibilities(
                principal.getUser().getUserId(), request);

        return CommonResponse.success(response, "활동 공개여부 변경 성공!");
    }

    @Operation(
            summary = "내 활동 목록 조회 (커서 기반)",
            description = """
                    본인이 생성한 레시피/데이터셋/Q&A/팀원 모집 게시글을 최신순으로 통합 조회합니다.
                    본인 조회이므로 공개/비공개, 마이페이지 노출 설정과 무관하게 소유한 항목을 모두 반환합니다.
                    cursor는 이전 응답의 nextCursor를 그대로 전달하면 되며, 최초 조회 시 생략합니다.

                    응답 items는 type별로 실제 존재하는 필드만 내려가는 서로 다른 구조입니다 (null로 채우지 않음).
                    - RECIPE(RecipeItem): thumbnailUrl, likeCount, commentCount(댓글+대댓글 합산), forkCount
                    - DATASET(DatasetItem): thumbnailUrl, likeCount, downloadCount
                    - QNA(QnaItem): commentCount(전문가 답변 수. 답변엔 대댓글 개념이 없어 전체 답변 수와 동일)
                    - RECRUITMENT(RecruitItem): commentCount(지원자 수, 상태 무관 전체 지원 건수)
                    모든 타입 공통: activityId, type, title, description, isPublic, isMyPageVisible, createdAt
                    - isPublic: 글 자체의 공개 여부
                    - isMyPageVisible: 공개 프로필에 표시되는지. 비공개 글은 항상 false (PATCH /users/me/activities/visibility로 변경)"""
    )
    @GetMapping("/mypage/activities")
    public CommonResponse<MyActivityListResponse> getMyActivities(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int size) {

        MyActivityListResponse response = myActivityService.getMyActivities(
                principal.getUser().getUserId(), cursor, size);

        return CommonResponse.success(response, "내 활동 목록 조회 성공!");
    }
}
