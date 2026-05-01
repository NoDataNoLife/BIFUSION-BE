package com.nodatanolife.bifusion_be.domain.comment.dto.response;

import com.nodatanolife.bifusion_be.domain.comment.entity.Comment;
import java.time.LocalDateTime;


public record CommentUpdateResponse(
        Long commentId,
        Long userId,
        String author,
        String profileImageUrl,
        String content,
        LocalDateTime updatedAt
) {
    public static CommentUpdateResponse from(Comment comment) {
        return new CommentUpdateResponse(
                comment.getCommentId(),
                comment.getUser().getUserId(),
                comment.getUser().getNickname(),
                comment.getUser().getProfileImageUrl(),
                comment.getContent(),
                comment.getUpdatedAt()
        );
    }
}