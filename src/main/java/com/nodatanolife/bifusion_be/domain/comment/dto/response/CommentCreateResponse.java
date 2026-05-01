package com.nodatanolife.bifusion_be.domain.comment.dto.response;

import com.nodatanolife.bifusion_be.domain.comment.entity.Comment;
import java.time.LocalDateTime;

public record CommentCreateResponse(
        Long commentId,
        Long userId,
        String author,
        String profileImageUrl,
        String content,
        LocalDateTime createdAt
) {
    public static CommentCreateResponse of(Comment comment) {
        return new CommentCreateResponse(
                comment.getCommentId(),
                comment.getUser().getUserId(),
                comment.getUser().getNickname(),
                comment.getUser().getProfileImageUrl(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}