package com.nodatanolife.bifusion_be.domain.user.entity;

import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "email")
    private String email;

    @Column(name = "name")
    private String name;

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "organization")
    private String organization;

    @Column(name = "contact", length = 20)
    private String contact;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "bio")
    private String bio;

    @Column(name = "role", length = 20)
    private String role;

    @Column(name = "website_url")
    private String websiteUrl;

    @Column(name = "is_expert")
    private Boolean isExpert;

    @Column(name = "is_onboarded")
    private Boolean isOnboarded;

    @Column(name = "remaining_tokens")
    private Long remainingTokens;

    @Column(name = "reward_balance")
    private Integer rewardBalance;

    /**
     * 구글 소셜 로그인으로 처음 가입하는 신규 유저 생성
     * role = USER, isExpert = false, isOnboarded = false 로 고정
     */
    public static User createOAuthUser(String email, String name) {
        User user = new User();
        user.email = email;
        user.name = name;
        user.profileImageUrl = null;
        user.role = "USER";
        user.isExpert = false;
        user.isOnboarded = false;
        user.remainingTokens = 0L;
        user.rewardBalance = 0;
        return user;
    }

    /**
     * 온보딩 완료 처리
     * 최초 로그인 후 추가 정보 입력 시 호출
     * isOnboarded = true 로 변경되어 이후 로그인 시 온보딩 페이지 스킵
     */
    public void onboard(String nickname, String organization, String contact) {
        this.nickname = nickname;
        this.organization = organization;
        this.contact = contact;
        this.isOnboarded = true;
    }

    /**
     * 탈퇴한 사용자 익명화용 더미 계정
     * 앱 시작 시 DataInitializer가 한 번만 생성
     * 탈퇴 유저의 콘텐츠는 이 계정으로 FK 교체
     */
    public static User createDeletedUser() {
        User user = new User();
        user.email = "deleted@bifusion.com";
        user.name = "탈퇴한 사용자";
        user.nickname = "탈퇴한 사용자";
        user.role = "DELETED";
        user.isExpert = false;
        user.isOnboarded = true;
        user.remainingTokens = 0L;
        user.rewardBalance = 0;
        return user;
    }

    /**
     * 닉네임 변경 (마이페이지에서 사용)
     */
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateBio(String bio) {
        this.bio = bio;
    }
}
