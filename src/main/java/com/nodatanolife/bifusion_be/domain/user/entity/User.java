package com.nodatanolife.bifusion_be.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

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

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateBio(String bio) {
        this.bio = bio;
    }
}
