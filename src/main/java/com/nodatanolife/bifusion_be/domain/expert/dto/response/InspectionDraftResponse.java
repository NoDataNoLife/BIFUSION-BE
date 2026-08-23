package com.nodatanolife.bifusion_be.domain.expert.dto.response;

import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertTask;

import java.time.LocalDateTime;

public record InspectionDraftResponse(
        Long inspectionId,
        String expertComment,
        LocalDateTime updatedAt
) {
    public static InspectionDraftResponse from(ExpertTask task) {
        return new InspectionDraftResponse(
                task.getExpertTaskId(),
                task.getDraftFeedback(),
                task.getUpdatedAt()
        );
    }
}
