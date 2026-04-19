package com.nodatanolife.bifusion_be.domain.auth.service;

import com.nodatanolife.bifusion_be.domain.auth.dto.AuthTokens;
import com.nodatanolife.bifusion_be.domain.user.entity.User;

public interface AuthService {

    AuthTokens rotate(String oldRefreshToken, String userAgent, String ipAddress);

    void logout(String refreshToken);

    void deleteByUser(User user);
}