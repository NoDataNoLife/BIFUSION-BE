package com.nodatanolife.bifusion_be.domain.user.service;


import com.nodatanolife.bifusion_be.domain.user.dto.response.*;
import com.nodatanolife.bifusion_be.domain.user.dto.request.OnboardingRequest;
import com.nodatanolife.bifusion_be.domain.user.entity.SubscriptionTier;
import org.springframework.web.multipart.MultipartFile;


public interface UserService {

    UserResponse getMe(Long userId);

    MyPageBasicResponse getMyPageBasic(Long userId);

    NicknameUpdateResponse updateNickname(Long userId, String nickname);

    BioUpdateResponse updateBio(Long userId, String bio);

    ProfileImageUpdateResponse updateProfileImage(Long userId, MultipartFile image);

    LocationUpdateResponse updateLocation(Long userId, String location);

    WebsiteUpdateResponse updateWebsiteUrl(Long userId, String websiteUrl);

    PlanChangeResponse changePlan(Long userId, SubscriptionTier newTier);

    UserResponse onboard(Long userId, OnboardingRequest request);

    void withdraw(Long userId);

    UserSearchResponse searchByEmail(String email);

}