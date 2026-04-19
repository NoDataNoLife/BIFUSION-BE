package com.nodatanolife.bifusion_be.domain.auth.dto;

public record AuthTokens(
        String accessToken,
        String refreshToken,
        long refreshMaxAge
) {}

