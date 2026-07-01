package com.nodatanolife.bifusion_be.domain.qna.dto.response;

import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
import com.nodatanolife.bifusion_be.domain.qna.entity.ExpertQnA;
import com.nodatanolife.bifusion_be.domain.qna.entity.ExpertQnAStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ExpertQnAListResponse(
        Long qnaId,
        String title,
        List<String> tags,
        ExpertQnAStatus status,
        Boolean isExpertAnswered,
        AuthorInfo author,
        Integer answerCount,
        LocalDateTime createdAt
) {
    public record AuthorInfo(Long userId, String nickname, @PresignedUrl String profileImageUrl) {}

    public static ExpertQnAListResponse from(ExpertQnA qna, List<String> tags) {
        return new ExpertQnAListResponse(
                qna.getExpertQnAId(),
                qna.getTitle(),
                tags,
                qna.getStatus(),
                qna.getHasExpertReply(),                new AuthorInfo(
                        qna.getUser().getUserId(),
                        qna.getUser().getNickname(),
                        qna.getUser().getProfileImageUrl()
                ),
                qna.getAnswerCount(),
                qna.getCreatedAt()
        );
    }
}
