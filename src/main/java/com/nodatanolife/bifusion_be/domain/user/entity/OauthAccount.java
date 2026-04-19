package com.nodatanolife.bifusion_be.domain.user.entity;

import com.nodatanolife.bifusion_be.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "oauth_account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OauthAccount extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oauth_account_id")
    private Long oauthAccountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "provider")
    private String provider;

    @Column(name = "provider_id")
    private String providerId;

    /**
     * 소셜 로그인 계정 연동 정보 생성
     * provider: "google" 등 OAuth 제공자 이름
     * providerId: 해당 제공자가 발급한 유저 고유 ID (구글은 sub 값)
     * 한 User가 여러 provider로 가입 가능한 구조를 지원
     */
    public static OauthAccount create(User user, String provider, String providerId) {
        OauthAccount account = new OauthAccount();
        account.user = user;
        account.provider = provider;
        account.providerId = providerId;
        return account;
    }
}
