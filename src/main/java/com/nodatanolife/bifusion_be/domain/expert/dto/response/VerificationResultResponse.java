package com.nodatanolife.bifusion_be.domain.expert.dto.response;

import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertVerification;

import java.time.LocalDateTime;

public record VerificationResultResponse(
        String reviewerName,
        Integer score,
        String comment,
        LocalDateTime verifiedAt
) {
    public static VerificationResultResponse from(ExpertVerification verification) {
        return new VerificationResultResponse(
                verification.getReviewedBy() != null ? verification.getReviewedBy().getName() : null,
                verification.getScore(),
                verification.getComment(),
                verification.getReviewedAt()
        );
    }
}
