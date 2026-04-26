package com.nodatanolife.bifusion_be.domain.user.controller;

import com.nodatanolife.bifusion_be.domain.user.entity.SubscriptionTier;
import com.nodatanolife.bifusion_be.domain.user.dto.request.BioUpdateRequest;
import com.nodatanolife.bifusion_be.domain.user.dto.request.PlanChangeRequest;
import com.nodatanolife.bifusion_be.domain.user.dto.response.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.multipart.MultipartFile;
import com.nodatanolife.bifusion_be.domain.user.dto.request.WebsiteUpdateRequest;
import com.nodatanolife.bifusion_be.domain.user.dto.request.LocationUpdateRequest;
import com.nodatanolife.bifusion_be.domain.user.dto.request.NicknameUpdateRequest;
import com.nodatanolife.bifusion_be.domain.user.dto.request.OnboardingRequest;
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
}
