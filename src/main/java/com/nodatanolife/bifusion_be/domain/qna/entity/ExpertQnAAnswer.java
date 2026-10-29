package com.nodatanolife.bifusion_be.domain.qna.entity;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "expert_qna_answer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpertQnAAnswer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expert_qna_answer_id")
    private Long expertQnAAnswerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expert_qna_id", nullable = false)
    private ExpertQnA expertQnA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder
    private ExpertQnAAnswer(ExpertQnA expertQnA, User user, String content) {
        this.expertQnA = expertQnA;
        this.user = user;
        this.content = content;
    }

    public static ExpertQnAAnswer create(ExpertQnA expertQnA, User user, String content) {
        return ExpertQnAAnswer.builder()
                .expertQnA(expertQnA)
                .user(user)
                .content(content)
                .build();
    }

    public void update(String content) {
        this.content = content;
    }

    public void delete() {
        markDeleted();
    }
}