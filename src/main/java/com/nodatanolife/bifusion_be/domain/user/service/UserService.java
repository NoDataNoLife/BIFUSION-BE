package com.nodatanolife.bifusion_be.domain.user.service;


import com.nodatanolife.bifusion_be.domain.user.dto.response.*;
import com.nodatanolife.bifusion_be.domain.user.dto.request.OnboardingRequest;
import org.springframework.web.multipart.MultipartFile;


public interface UserService {

    UserResponse getMe(Long userId);

    NicknameUpdateResponse updateNickname(Long userId, String nickname);

    BioUpdateResponse updateBio(Long userId, String bio);

    ProfileImageUpdateResponse updateProfileImage(Long userId, MultipartFile image);

    LocationUpdateResponse updateLocation(Long userId, String location);

    WebsiteUpdateResponse updateWebsiteUrl(Long userId, String websiteUrl);

    UserResponse onboard(Long userId, OnboardingRequest request);

    void withdraw(Long userId);

    UserSearchResponse searchByEmail(String email);
}