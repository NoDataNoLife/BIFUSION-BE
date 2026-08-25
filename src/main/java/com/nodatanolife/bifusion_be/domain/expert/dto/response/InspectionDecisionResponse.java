package com.nodatanolife.bifusion_be.domain.expert.dto.response;

import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertTask;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequest;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequestStatus;

import java.time.LocalDateTime;

public record InspectionDecisionResponse(
        Long inspectionId,
        InspectionRequestStatus status,
        String expertComment,
        LocalDateTime reviewedAt
) {
    public static InspectionDecisionResponse from(ExpertTask task, InspectionRequest inspectionRequest) {
        return new InspectionDecisionResponse(
                task.getExpertTaskId(),
                inspectionRequest.getStatus(),
                inspectionRequest.getComment(),
                inspectionRequest.getReviewedAt()
        );
    }
}
