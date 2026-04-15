package com.nodatanolife.bifusion_be.domain.recruitment.entity;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recruit_application")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recruit_application_id")
    private Long recruitApplicationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruit_id", nullable = false)
    private Recruit recruit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "email")
    private String email;

    @Column(name = "resume_url")
    private String resumeUrl;

    @Column(name = "motivation")
    private String motivation;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
