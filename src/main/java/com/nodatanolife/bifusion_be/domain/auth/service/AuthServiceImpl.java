package com.nodatanolife.bifusion_be.domain.auth.service;

import com.nodatanolife.bifusion_be.domain.auth.dto.AuthTokens;
import com.nodatanolife.bifusion_be.domain.user.entity.RefreshToken;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.repository.OauthAccountRepository;
import com.nodatanolife.bifusion_be.domain.user.repository.RefreshTokenRepository;
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import com.nodatanolife.bifusion_be.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OauthAccountRepository oauthAccountRepository;
    private final UserRepository userRepository;

    /**
     * Refresh Token Rotation
     * 구버전 토큰 삭제 후 신규 토큰 발급
     * DB에 없는 토큰으로 요청 시 탈취 감지 → 해당 유저 전체 토큰 폐기
     */
    @Override
    public AuthTokens rotate(String oldRefreshToken, String userAgent, String ipAddress) {
        RefreshToken stored = refreshTokenRepository
                .findByTokenAndIsRevokedFalse(oldRefreshToken)
                .orElseThrow(() -> {
                    tryRevokeAll(oldRefreshToken);
                    return new BusinessException(ErrorCode.INVALID_TOKEN);
                });

        if (stored.isExpired()) {
            refreshTokenRepository.delete(stored);
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        }

        User user = stored.getUser();
        String newAccessToken  = jwtTokenProvider.generateAccessToken(user);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);
        long maxAge = jwtTokenProvider.getRefreshExpirationSeconds();

        refreshTokenRepository.delete(stored);
        refreshTokenRepository.save(RefreshToken.create(
                user, newRefreshToken, userAgent, ipAddress,
                LocalDateTime.now().plusSeconds(maxAge)
        ));

        return new AuthTokens(newAccessToken, newRefreshToken, maxAge);
    }

    /**
     * 로그아웃
     * Refresh Token DB에서 삭제
     */
    @Override
    public void logout(String refreshToken) {
        refreshTokenRepository.findByTokenAndIsRevokedFalse(refreshToken)
                .ifPresent(refreshTokenRepository::delete);
    }

    /**
     * 회원 탈퇴 시 인증 정보 삭제
     * Refresh Token 전체 삭제 및 OauthAccount 삭제
     */
    @Override
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteAllByUser(user);
        oauthAccountRepository.deleteByUser(user);
    }

    /**
     * 탈취 감지 시 해당 유저의 모든 Refresh Token 폐기
     * 만료된 토큰이라 파싱 실패할 수 있으므로 try-catch 처리
     */
    private void tryRevokeAll(String token) {
        try {
            String email = jwtTokenProvider.getEmail(token);
            userRepository.findByEmail(email)
                    .ifPresent(refreshTokenRepository::deleteAllByUser);
        } catch (Exception e) {
            log.warn("탈취 감지 중 오류 발생: {}", e.getMessage());
        }
    }
}