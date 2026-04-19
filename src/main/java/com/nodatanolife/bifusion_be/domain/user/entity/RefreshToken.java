package com.nodatanolife.bifusion_be.domain.user.entity;

import com.nodatanolife.bifusion_be.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_token_id")
    private Long refreshTokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "user_agent")
    private String userAgent; // 토큰 발급 시 접속 기기/브라우저 정보

    @Column(name = "ip_address", length = 45)
    private String ipAddress; // 토큰 발급 시 IP (IPv6 대비 45자)

    @Column(name = "token")
    private String token;

    @Column(name = "is_revoked")
    private Boolean isRevoked; // true면 폐기된 토큰 (탈취 감지, 강제 로그아웃)

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * 로그인 성공 시 Refresh Token 발급
     * isRevoked = false 로 고정 (생성 시점엔 항상 유효)
     * userAgent, ipAddress는 탈취 감지 보조 수단으로 저장
     */
    public static RefreshToken create(User user, String token,
                                      String userAgent, String ipAddress,
                                      LocalDateTime expiresAt) {
        RefreshToken rt = new RefreshToken();
        rt.user = user;
        rt.token = token;
        rt.userAgent = userAgent;
        rt.ipAddress = ipAddress;
        rt.isRevoked = false;
        rt.expiresAt = expiresAt;
        return rt;
    }

    /**
     * 토큰 만료 여부 확인
     * expiresAt이 현재 시각보다 이전이면 만료
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

}
