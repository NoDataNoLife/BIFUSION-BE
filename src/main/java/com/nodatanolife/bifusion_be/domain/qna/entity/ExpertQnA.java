package com.nodatanolife.bifusion_be.domain.qna.entity;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "expert_qna")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpertQnA extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expert_qna_id")
    private Long expertQnAId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title")
    private String title;

    @Column(name = "content")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ExpertQnAStatus status;

    @Column(name = "has_expert_reply")
    private Boolean hasExpertReply;

    @Column(name = "answer_count")
    private Integer answerCount;

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

    @Builder
    private ExpertQnA(User user, String title, String content) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.status = ExpertQnAStatus.OPEN;
        this.hasExpertReply = false;
        this.answerCount = 0;
        this.isPublic = true;
        this.isMyPageVisible = true;
    }

    public static ExpertQnA create(User user, String title, String content) {
        return ExpertQnA.builder()
                .user(user)
                .title(title)
                .content(content)
                .build();
    }

    public void addExpertReply() {
        this.hasExpertReply = true;
        this.answerCount += 1;
    }
}
