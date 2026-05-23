package com.nodatanolife.bifusion_be.domain.qna.dto.response;

import com.nodatanolife.bifusion_be.domain.qna.entity.ExpertQnA;

import java.time.LocalDateTime;
import java.util.List;

public record ExpertQnaResponse(
        Long qnaId,
        String title,
        String content,
        List<String> tags,
        LocalDateTime createdAt
) {
    public static ExpertQnaResponse from(ExpertQnA qna, List<String> tags) {
        return new ExpertQnaResponse(
                qna.getExpertQnAId(),
                qna.getTitle(),
                qna.getContent(),
                tags,
                qna.getCreatedAt()
        );
    }
}