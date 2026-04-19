package com.nodatanolife.bifusion_be.domain.user.dto.request;

public record OnboardingRequest(
        String nickname,
        String organization,
        String contact
) {}
