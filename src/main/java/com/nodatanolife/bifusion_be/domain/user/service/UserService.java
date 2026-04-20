package com.nodatanolife.bifusion_be.domain.user.service;


import com.nodatanolife.bifusion_be.domain.user.dto.response.BioUpdateResponse;
import com.nodatanolife.bifusion_be.domain.user.dto.request.OnboardingRequest;
import com.nodatanolife.bifusion_be.domain.user.dto.response.LocationUpdateResponse;
import com.nodatanolife.bifusion_be.domain.user.dto.response.NicknameUpdateResponse;
import com.nodatanolife.bifusion_be.domain.user.dto.response.UserResponse;
import com.nodatanolife.bifusion_be.domain.user.dto.response.WebsiteUpdateResponse;


public interface UserService {

    UserResponse getMe(Long userId);

    NicknameUpdateResponse updateNickname(Long userId, String nickname);
    BioUpdateResponse updateBio(Long userId, String bio);

    LocationUpdateResponse updateLocation(Long userId, String location);

    WebsiteUpdateResponse updateWebsiteUrl(Long userId, String websiteUrl);

    UserResponse onboard(Long userId, OnboardingRequest request);

    void withdraw(Long userId);
}