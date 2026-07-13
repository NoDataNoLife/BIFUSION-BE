package com.nodatanolife.bifusion_be.domain.recruitment.entity;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "recruit_application")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitApplication extends BaseEntity {

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


    @Builder
    private RecruitApplication(Recruit recruit, User user, String fullName, String email,
                               String resumeUrl, String motivation) {
        this.recruit = recruit;
        this.user = user;
        this.fullName = fullName;
        this.email = email;
        this.resumeUrl = resumeUrl;
        this.motivation = motivation;
        this.status = "PENDING";
    }

    public static RecruitApplication create(Recruit recruit, User user, String fullName,
                                            String email, String resumeUrl, String motivation) {
        return RecruitApplication.builder()
                .recruit(recruit)
                .user(user)
                .fullName(fullName)
                .email(email)
                .resumeUrl(resumeUrl)
                .motivation(motivation)
                .build();
    }


    public void anonymize(User deletedUser) {
        this.user = deletedUser;
    }

    public void updateStatus(RecruitApplicationStatus status) {
        this.status = status.name();
    }
}
