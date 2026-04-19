package com.nodatanolife.bifusion_be.domain.user.service;


import com.nodatanolife.bifusion_be.domain.user.dto.response.BioUpdateResponse;
import com.nodatanolife.bifusion_be.domain.user.dto.request.OnboardingRequest;
import com.nodatanolife.bifusion_be.domain.user.dto.response.NicknameUpdateResponse;
import com.nodatanolife.bifusion_be.domain.user.dto.response.UserResponse;


public interface UserService {

    UserResponse getMe(Long userId);

    NicknameUpdateResponse updateNickname(Long userId, String nickname);
    BioUpdateResponse updateBio(Long userId, String bio);

    UserResponse onboard(Long userId, OnboardingRequest request);

    void withdraw(Long userId);
}