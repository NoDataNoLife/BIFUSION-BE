package com.nodatanolife.bifusion_be.domain.user.controller;

import com.nodatanolife.bifusion_be.domain.user.dto.request.BioUpdateRequest;
import com.nodatanolife.bifusion_be.domain.user.dto.request.PlanChangeRequest;
import com.nodatanolife.bifusion_be.domain.user.dto.response.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.multipart.MultipartFile;
import com.nodatanolife.bifusion_be.domain.user.dto.request.WebsiteUpdateRequest;
import com.nodatanolife.bifusion_be.domain.user.dto.request.LocationUpdateRequest;
import com.nodatanolife.bifusion_be.domain.user.dto.request.NicknameUpdateRequest;
import com.nodatanolife.bifusion_be.domain.user.dto.request.OnboardingRequest;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.ExpertApplicationResponse;
import com.nodatanolife.bifusion_be.domain.expert.service.ExpertService;
import com.nodatanolife.bifusion_be.domain.project.dto.request.ProjectVisibilityUpdateRequest;
import com.nodatanolife.bifusion_be.domain.project.dto.response.ProjectVisibilityUpdateResponse;
import com.nodatanolife.bifusion_be.domain.project.service.ProjectService;
import com.nodatanolife.bifusion_be.domain.user.dto.request.ActivityVisibilityUpdateRequest;
import com.nodatanolife.bifusion_be.domain.user.dto.response.ActivityVisibilityUpdateResponse;
import com.nodatanolife.bifusion_be.domain.user.service.MyActivityVisibilityService;
import com.nodatanolife.bifusion_be.domain.user.service.UserService;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import com.nodatanolife.bifusion_be.global.security.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class UserController {

    private final UserService userService;
    private final ExpertService expertService;
    private final ProjectService projectService;
    private final MyActivityVisibilityService myActivityVisibilityService;
    private final CookieUtil cookieUtil;

    /**
     * 내 정보 조회
     * JWT 쿠키로 인증된 유저 본인의 프로필 정보 반환
     */
    @GetMapping("/users/me")
    public CommonResponse<UserResponse> getMe(
            @AuthenticationPrincipal CustomOAuth2User principal) {
        UserResponse response = userService.getMe(principal.getUser().getUserId());
        return CommonResponse.success(response);
    }

    /**
     * 마이페이지 기본 정보 조회
     */
    @GetMapping("/users/{userId}")
    public CommonResponse<MyPageBasicResponse> getMyPageBasic(
            @PathVariable Long userId) {
        MyPageBasicResponse response = userService.getMyPageBasic(userId);
        return CommonResponse.success(response, "마이페이지 조회 성공!");
    }

    /**
     * 닉네임 수정
     * 인증된 유저의 닉네임 변경
     */
    @PutMapping("/profile/nickname")
    public CommonResponse<NicknameUpdateResponse> updateNickname(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestBody NicknameUpdateRequest request) {

        NicknameUpdateResponse response = userService.updateNickname(
                principal.getUser().getUserId(), request.getNickname());
        return CommonResponse.success(response);
    }



    /**
     * 소개글 수정
     */
    @PutMapping("/profile/introduction")
    public CommonResponse<BioUpdateResponse> updateBio(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestBody BioUpdateRequest request) {
        BioUpdateResponse response = userService.updateBio(
                principal.getUser().getUserId(), request.getBio());
        return CommonResponse.success(response, "소개글이 수정되었습니다.");
    }

    /**
     * 위치 수정
     * 인증된 유저의 위치 변경
     */
    @PutMapping("/profile/location")
    public CommonResponse<LocationUpdateResponse> updateLocation(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestBody LocationUpdateRequest request) {
        LocationUpdateResponse response = userService.updateLocation(
                principal.getUser().getUserId(), request.getLocation());
        return CommonResponse.success(response, "위치 수정 완료!");
    }

    /**
     * 웹사이트 수정
     * 인증된 유저의 웹사이트 변경
     */
    @PutMapping("/profile/website")
    public CommonResponse<WebsiteUpdateResponse> updateWebsiteUrl(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestBody WebsiteUpdateRequest request) {
        WebsiteUpdateResponse response = userService.updateWebsiteUrl(
                principal.getUser().getUserId(), request.getWebsiteUrl());
        return CommonResponse.success(response, "웹사이트가 수정되었습니다.");
    }

    /**
     * 프로필사진 수정
     */
    @PutMapping("/profile/image")
    public CommonResponse<ProfileImageUpdateResponse> updateProfileImage(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestPart MultipartFile image) {
        ProfileImageUpdateResponse response = userService.updateProfileImage(
                principal.getUser().getUserId(), image);
        return CommonResponse.success(response, "프로필 사진이 수정되었습니다.");
    }

    @PutMapping("/users/me/plan")
    public CommonResponse<PlanChangeResponse> changePlan(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestBody PlanChangeRequest request) {

        PlanChangeResponse response = userService.changePlan(principal.getUser().getUserId(), request.planType());

        return CommonResponse.success(response);
    }
    /**
     * 온보딩 완료 처리
     * 최초 로그인 후 추가 정보 입력 시 호출
     */
    @PostMapping("/onboarding")
    public CommonResponse<UserResponse> onboarding(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestBody OnboardingRequest request) {

        UserResponse response = userService.onboard(
                principal.getUser().getUserId(), request);

        return CommonResponse.success(response, "회원가입 성공!");
    }

    /**
     * 회원 탈퇴
     * 인증 정보 삭제 → 개인 기록 삭제 → 공개 콘텐츠 익명화 → User 삭제
     */
    @DeleteMapping("/users/me")
    public CommonResponse<Void> withdraw(
            @AuthenticationPrincipal CustomOAuth2User principal,
            HttpServletResponse response) {

        userService.withdraw(principal.getUser().getUserId());

        // 쿠키 만료 처리
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieUtil.expireCookie("access_token").toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieUtil.expireCookie("refresh_token").toString());

        return CommonResponse.success(null, "회원 탈퇴 성공!");
    }

    /**
     * 이메일로 유저 검색
     * 이메일 기반으로 유저를 조회하며, 미가입 이메일인 경우 exists: false 반환
     */
    @GetMapping("/users/search")
    public CommonResponse<UserSearchResponse> searchByEmail(
            @RequestParam @Email @NotBlank String email
    ) {
        UserSearchResponse response = userService.searchByEmail(email);
        return CommonResponse.success(response, "유저 이메일 검색 성공!");
    }

    /**
     * 전문가 인증 신청
     * 의료 면허증 또는 학위 증명서를 업로드해 PENDING 상태로 신청을 등록
     */
    @PostMapping("/users/me/expert")
    public CommonResponse<ExpertApplicationResponse> applyExpert(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestPart MultipartFile certificationFile) {

        ExpertApplicationResponse response = expertService.apply(
                principal.getUser().getUserId(), certificationFile);

        return CommonResponse.success(response, "전문가 인증 신청이 완료되었습니다!");
    }

    /**
     * 마이페이지에서 내가 참여한 프로젝트들의 공개 여부 일괄 변경
     * 비공개 프로젝트는 공개로 변경 불가
     */
    @PutMapping("/users/me/projects/visibility")
    public CommonResponse<ProjectVisibilityUpdateResponse> updateMyProjectVisibilities(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestBody @Valid ProjectVisibilityUpdateRequest request) {

        ProjectVisibilityUpdateResponse response = projectService.updateMyProjectVisibilities(
                principal.getUser().getUserId(), request);

        return CommonResponse.success(response, "프로젝트 공개 설정을 변경했습니다.");
    }

    /**
     * 마이페이지 커뮤니티 활동(Recipe / Dataset / Q&A / Recruitment) 공개 여부 일괄 변경
     * - 항목별 type + id + isPublic 로 한 번에 처리 (단건/다건 모두 지원)
     * - 콘텐츠 자체가 비공개면 마이페이지에도 강제 비공개로 저장
     * - 본인 소유가 아닌 항목이 있으면 전체 롤백
     */
    @PatchMapping("/users/me/activities/visibility")
    public CommonResponse<ActivityVisibilityUpdateResponse> updateMyActivityVisibilities(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestBody @Valid ActivityVisibilityUpdateRequest request) {

        ActivityVisibilityUpdateResponse response = myActivityVisibilityService.updateVisibilities(
                principal.getUser().getUserId(), request);

        return CommonResponse.success(response, "활동 공개 설정 변경 성공!");
    }
}
