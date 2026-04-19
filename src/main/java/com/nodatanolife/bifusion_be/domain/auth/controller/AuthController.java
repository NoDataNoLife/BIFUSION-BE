package com.nodatanolife.bifusion_be.domain.auth.controller;

import com.nodatanolife.bifusion_be.domain.auth.dto.AuthTokens;
import com.nodatanolife.bifusion_be.domain.auth.service.AuthService;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.util.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;

    /**
     * 토큰 갱신
     * Refresh Token Rotation으로 새 Access/Refresh Token 쌍 발급
     */
    @PostMapping("/refresh")
    public CommonResponse<Void> refresh(HttpServletRequest request,
                                        HttpServletResponse response) {
        String refreshToken = extractRefreshToken(request)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

        AuthTokens tokens = authService.rotate(
                refreshToken,
                request.getHeader("User-Agent"),
                request.getRemoteAddr()
        );

        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieUtil.createAccessTokenCookie(tokens.accessToken()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieUtil.createRefreshTokenCookie(tokens.refreshToken(), tokens.refreshMaxAge()).toString());

        return CommonResponse.success(null);
    }

    /**
     * 로그아웃
     * Refresh Token DB에서 삭제 및 쿠키 만료 처리
     */
    @PostMapping("/logout")
    public CommonResponse<Void> logout(HttpServletRequest request,
                                       HttpServletResponse response) {
        extractRefreshToken(request)
                .ifPresent(authService::logout);

        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieUtil.expireCookie("access_token").toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieUtil.expireCookie("refresh_token").toString());

        return CommonResponse.success(null);
    }

    private Optional<String> extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(c -> c.getName().equals("refresh_token"))
                .map(Cookie::getValue)
                .findFirst();
    }
}