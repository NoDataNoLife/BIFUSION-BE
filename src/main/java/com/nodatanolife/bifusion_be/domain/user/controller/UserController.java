package com.nodatanolife.bifusion_be.domain.user.controller;

import com.nodatanolife.bifusion_be.domain.user.dto.request.NicknameUpdateRequest;
import com.nodatanolife.bifusion_be.domain.user.dto.request.OnboardingRequest;
import com.nodatanolife.bifusion_be.domain.user.dto.response.NicknameUpdateResponse;
import com.nodatanolife.bifusion_be.domain.user.dto.response.UserResponse;
import com.nodatanolife.bifusion_be.domain.user.service.UserService;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/")
public class UserController {

    private final UserService userService;

    @GetMapping("/users/me")
    public CommonResponse<UserResponse> getMe(
            @AuthenticationPrincipal CustomOAuth2User principal) {
        UserResponse response = userService.getMe(principal.getUser().getUserId());
        return CommonResponse.success(response);
    }

    @PutMapping("/profile/nickname")
    public CommonResponse<NicknameUpdateResponse> updateNickname(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestBody NicknameUpdateRequest request) {
        NicknameUpdateResponse response = userService.updateNickname(
                principal.getUser().getUserId(), request.getNickname());
        return CommonResponse.success(response);
    }

    @PostMapping("/onboarding")
    public CommonResponse<UserResponse> onboarding(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestBody OnboardingRequest request) {

        UserResponse response = userService.onboard(
                principal.getUser().getUserId(), request);

        return CommonResponse.success(response, "회원가입 성공!");
    }
}