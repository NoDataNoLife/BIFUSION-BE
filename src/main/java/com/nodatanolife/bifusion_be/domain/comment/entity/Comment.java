package com.nodatanolife.bifusion_be.domain.comment.entity;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 다형성 댓글 테이블.
 * target_type: "RECIPE" | "EXPERT_QNA" | "DATASET" 등
 */
@Entity
@Table(name = "comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long commentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "content")
    private String content;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "target_type", length = 20)
    private String targetType;

    @Column(name = "is_expert_reply")
    private Boolean isExpertReply;

    @Column(name = "is_accepted")
    private Boolean isAccepted;

    public static Comment create(User user, Long targetId, String targetType, String content) {
        Comment comment = new Comment();
        comment.user = user;
        comment.targetId = targetId;
        comment.targetType = targetType;
        comment.content = content;
        comment.isExpertReply = false;
        comment.isAccepted = false;
        return comment;
    }

    public void anonymize(User deletedUser) {
        this.user = deletedUser;
    }
}
