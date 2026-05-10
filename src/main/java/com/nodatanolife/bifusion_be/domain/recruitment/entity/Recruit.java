package com.nodatanolife.bifusion_be.domain.recruitment.entity;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recruit")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recruit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recruit_id")
    private Long recruitId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title")
    private String title;

    @Column(name = "organization")
    private String organization;

    @Column(name = "content")
    private String content;

    @Column(name = "requirements")
    private String requirements;

    @Column(name = "responsibilities")
    private String responsibilities;

    @Column(name = "benefits")
    private String benefits;

    @Column(name = "team_size")
    private Integer teamSize;

    @Column(name = "deadline")
    private LocalDateTime deadline;

    @Column(name = "is_public")
    private Boolean isPublic;

    @Column(name = "is_mypage_visible")
    private Boolean isMyPageVisible;

    public void anonymize(User deletedUser) {
        this.user = deletedUser;
    }

    /**
     * 마이페이지 활동 공개 여부 변경
     */
    public void updateMyPageVisibility(boolean visible) {
        this.isMyPageVisible = visible;
    }
}
