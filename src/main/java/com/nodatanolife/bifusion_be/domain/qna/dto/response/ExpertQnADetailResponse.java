package com.nodatanolife.bifusion_be.domain.qna.dto.response;

import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
import com.nodatanolife.bifusion_be.domain.qna.entity.ExpertQnA;
import com.nodatanolife.bifusion_be.domain.qna.entity.ExpertQnAAnswer;
import com.nodatanolife.bifusion_be.domain.qna.entity.ExpertQnAStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ExpertQnADetailResponse(
        Long qnaId,
        String title,
        String content,
        List<String> tags,
        ExpertQnAStatus status,
        Boolean isExpertAnswered,
        AuthorInfo author,
        Integer answerCount,
        List<AnswerInfo> answers,
        LocalDateTime createdAt
) {
    public record AuthorInfo(Long userId, String nickname, @PresignedUrl String profileImageUrl) {}

    public record AnswerInfo(
            Long answerId,
            String content,
            AuthorInfo author,
            LocalDateTime createdAt
    ) {}

    public static ExpertQnADetailResponse from(ExpertQnA qna, List<String> tags, List<ExpertQnAAnswer> answers) {
        return new ExpertQnADetailResponse(
                qna.getExpertQnAId(),
                qna.getTitle(),
                qna.getContent(),
                tags,
                qna.getStatus(),
                qna.getHasExpertReply(),
                new AuthorInfo(
                        qna.getUser().getUserId(),
                        qna.getUser().getNickname(),
                        qna.getUser().getProfileImageUrl()
                ),
                qna.getAnswerCount(),
                answers.stream()
                        .map(a -> new AnswerInfo(
                                a.getExpertQnAAnswerId(),
                                a.getContent(),
                                new AuthorInfo(
                                        a.getUser().getUserId(),
                                        a.getUser().getNickname(),
                                        a.getUser().getProfileImageUrl()
                                ),
                                a.getCreatedAt()
                        ))
                        .toList(),
                qna.getCreatedAt()
        );
    }
}