package com.nodatanolife.bifusion_be.domain.user.dto.request;

import jakarta.validation.constraints.Size;

public record OnboardingRequest(
        @Size(max = 255, message = "닉네임은 255자 이하로 입력해 주세요.")
        String nickname,
        @Size(max = 255, message = "소속은 255자 이하로 입력해 주세요.")
        String organization,
        @Size(max = 20, message = "연락처는 20자 이하로 입력해 주세요.")
        String contact
) {}
