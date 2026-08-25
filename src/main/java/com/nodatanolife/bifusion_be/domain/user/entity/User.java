package com.nodatanolife.bifusion_be.domain.user.entity;

import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

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

    @Column(name = "location")
    private String location;

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

    @Column(name = "is_onboarded")
    private Boolean isOnboarded;

    @Column(name = "remaining_tokens")
    private Long remainingTokens;

    @Column(name = "reward_balance")
    private Integer points;
    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", length = 10)
    private SubscriptionTier planType;


    @Column(name = "plan_start_date")
    private LocalDate planStartDate;

    /**
     * 구글 소셜 로그인으로 처음 가입하는 신규 유저 생성
     * role = USER, isOnboarded = false 로 고정
     */
    public static User createOAuthUser(String email, String name) {
        User user = new User();
        user.email = email;
        user.name = name;
        user.profileImageUrl = null;
        user.role = "USER";
        user.isOnboarded = false;
        user.remainingTokens = 0L;
        user.points = 0;
        user.planType = SubscriptionTier.BASIC;
        user.planStartDate = null;
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
        user.isOnboarded = true;
        user.remainingTokens = 0L;
        user.points = 0;
        return user;
    }


    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
    public void updateBio(String bio) {
        this.bio = bio;
    }
    public void updateLocation(String location) {
        this.location = location;
    }
    public void updateWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }
    public void updateProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    /**
     * 전문가 검수 신청 시 리워드 포인트 차감
     * reward_balance 컬럼이 NULL인 레거시 데이터를 방어하기 위해 null을 0으로 취급한다.
     */
    public void deductPoints(int amount) {
        int current = this.points == null ? 0 : this.points;
        this.points = current - amount;
    }

    /**
     * 전문가 검수 완료(승인/거절) 시 리워드 포인트 지급
     * reward_balance 컬럼이 NULL인 레거시 데이터를 방어하기 위해 null을 0으로 취급한다.
     */
    public void addPoints(int amount) {
        int current = this.points == null ? 0 : this.points;
        this.points = current + amount;
    }
    /**
     * 플랜 변경
     */
    public Subscription changePlan(SubscriptionTier newTier, long updatedTokens) {
        this.remainingTokens = updatedTokens;
        this.planType = newTier;
        this.planStartDate = LocalDate.now();

        return Subscription.builder()
                .user(this)
                .tier(newTier.name())
                .startedAt(java.time.LocalDateTime.now())
                .build();
    }
}
