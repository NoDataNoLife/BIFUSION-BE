package com.nodatanolife.bifusion_be.domain.qna.dto.response;

import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
import com.nodatanolife.bifusion_be.domain.qna.entity.ExpertQnAAnswer;
import java.time.LocalDateTime;

public record ExpertQnAAnswerResponse(
        Long answerId,
        Long qnaId,
        String content,
        Boolean isExpertAnswered,
        AuthorInfo author,
        LocalDateTime createdAt
) {
    public record AuthorInfo(
            Long userId,
            String nickname,
            @PresignedUrl String profileImageUrl,
            Boolean isExpert
    ) {}

    public static ExpertQnAAnswerResponse from(ExpertQnAAnswer answer, boolean isExpert) {
        return new ExpertQnAAnswerResponse(
                answer.getExpertQnAAnswerId(),
                answer.getExpertQnA().getExpertQnAId(),
                answer.getContent(),
                answer.getExpertQnA().getHasExpertReply(),
                new AuthorInfo(
                        answer.getUser().getUserId(),
                        answer.getUser().getNickname(),
                        answer.getUser().getProfileImageUrl(),
                        isExpert
                ),
                answer.getCreatedAt()
        );
    }
}