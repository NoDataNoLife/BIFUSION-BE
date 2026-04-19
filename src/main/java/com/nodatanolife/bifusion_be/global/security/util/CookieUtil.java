package com.nodatanolife.bifusion_be.global.security.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    private final long accessExpiration;
    private final boolean secureCookie;

    public CookieUtil(@Value("${jwt.access-expiration}") long accessExpiration,
                      @Value("${app.cookie.secure}") boolean secureCookie) {
        this.accessExpiration = accessExpiration;
        this.secureCookie = secureCookie;
    }

    public ResponseCookie createAccessTokenCookie(String token) {
        return ResponseCookie.from("access_token", token)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(accessExpiration / 1000)
                .build();
    }

    public ResponseCookie createRefreshTokenCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from("refresh_token", token)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }

    public ResponseCookie expireCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }
}