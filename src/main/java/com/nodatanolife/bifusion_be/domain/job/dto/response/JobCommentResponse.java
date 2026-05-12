package com.nodatanolife.bifusion_be.domain.job.dto.response;

import com.nodatanolife.bifusion_be.domain.job.entity.JobComment;
import java.time.LocalDateTime;

public record JobCommentResponse(
        Long commentId,
        Long userId,
        String author,
        String profileImageUrl,
        String content,
        LocalDateTime createdAt
) {
    public static JobCommentResponse from(JobComment comment) {
        return new JobCommentResponse(
                comment.getId(),
                comment.getUser().getUserId(),
                comment.getUser().getNickname(),
                comment.getUser().getProfileImageUrl(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}
