package com.nodatanolife.bifusion_be.global.security.oauth2;

import com.nodatanolife.bifusion_be.domain.user.entity.RefreshToken;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.repository.RefreshTokenRepository;
import com.nodatanolife.bifusion_be.global.security.jwt.JwtTokenProvider;
import com.nodatanolife.bifusion_be.global.security.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CookieUtil cookieUtil;

    @Value("${app.oauth2.frontend-uri}")
    private String frontendUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oAuth2User.getUser();

        String accessToken  = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        long maxAge = jwtTokenProvider.getRefreshExpirationSeconds();

        refreshTokenRepository.deleteByUser(user);
        refreshTokenRepository.save(RefreshToken.create(
                user, refreshToken,
                request.getHeader("User-Agent"),
                request.getRemoteAddr(),
                LocalDateTime.now().plusSeconds(maxAge)
        ));

        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieUtil.createAccessTokenCookie(accessToken).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieUtil.createRefreshTokenCookie(refreshToken, maxAge).toString());

        // isOnboarded 여부로 redirect 분기
        String target = user.getIsOnboarded()
                ? frontendUri
                : frontendUri + "/onboarding";

        getRedirectStrategy().sendRedirect(request, response, target);
    }
}