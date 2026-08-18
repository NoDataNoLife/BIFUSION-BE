package com.nodatanolife.bifusion_be.domain.expert.dto.response;

import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequest;

import java.time.LocalDateTime;

public record InspectionResultResponse(
        String reviewerName,
        Integer score,
        String comment,
        LocalDateTime verifiedAt
) {
    public static InspectionResultResponse from(InspectionRequest verification) {
        return new InspectionResultResponse(
                verification.getReviewedBy() != null ? verification.getReviewedBy().getName() : null,
                verification.getScore(),
                verification.getComment(),
                verification.getReviewedAt()
        );
    }
}
