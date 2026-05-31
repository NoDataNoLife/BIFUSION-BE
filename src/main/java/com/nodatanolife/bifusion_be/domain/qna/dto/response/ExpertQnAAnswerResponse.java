package com.nodatanolife.bifusion_be.domain.qna.dto.response;

import com.nodatanolife.bifusion_be.domain.qna.entity.ExpertQnAAnswer;

import java.time.LocalDateTime;

public record ExpertQnAAnswerResponse(
        Long answerId,
        Long qnaId,
        String content,
        AuthorInfo author,
        LocalDateTime createdAt
) {
    public record AuthorInfo(
            Long userId,
            String nickname,
            String profileImageUrl,
            Boolean isExpert
    ) {}

    public static ExpertQnAAnswerResponse from(ExpertQnAAnswer answer) {
        return new ExpertQnAAnswerResponse(
                answer.getExpertQnAAnswerId(),
                answer.getExpertQnA().getExpertQnAId(),
                answer.getContent(),
                new AuthorInfo(
                        answer.getUser().getUserId(),
                        answer.getUser().getNickname(),
                        answer.getUser().getProfileImageUrl(),
                        answer.getUser().getIsExpert()
                ),
                answer.getCreatedAt()
        );
    }
}