package com.nodatanolife.bifusion_be.domain.expert.dto.response;

import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequest;

import java.time.LocalDateTime;

public record InspectionResultResponse(
        String reviewerName,
        Integer score,
        String comment,
        LocalDateTime verifiedAt
) {
    public static InspectionResultResponse from(InspectionRequest inspectionRequest) {
        return new InspectionResultResponse(
                inspectionRequest.getReviewedBy() != null ? inspectionRequest.getReviewedBy().getName() : null,
                inspectionRequest.getScore(),
                inspectionRequest.getComment(),
                inspectionRequest.getReviewedAt()
        );
    }
}
