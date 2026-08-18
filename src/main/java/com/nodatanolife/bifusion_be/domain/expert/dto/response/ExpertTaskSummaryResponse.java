package com.nodatanolife.bifusion_be.domain.expert.dto.response;

import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertTask;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequest;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequestStatus;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationConfig;
import com.nodatanolife.bifusion_be.domain.user.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExpertTaskSummaryResponse(
        String reviewCode,
        String status,
        InspectionRequestStatus reviewResult,
        RequesterResponse requester,
        String projectName,
        LocalDate requestedAt,
        LocalDateTime updatedAt
) {
    public static ExpertTaskSummaryResponse of(ExpertTask task, AugmentationConfig config) {
        InspectionRequest inspectionRequest = task.getInspectionRequest();
        return new ExpertTaskSummaryResponse(
                task.getReviewCode(),
                task.getStatus(),
                inspectionRequest.getStatus() == InspectionRequestStatus.PENDING ? null : inspectionRequest.getStatus(),
                RequesterResponse.from(inspectionRequest.getUser()),
                config.getJob().getProject().getTitle(),
                inspectionRequest.getAppliedAt() != null ? inspectionRequest.getAppliedAt().toLocalDate() : null,
                task.getUpdatedAt()
        );
    }

    public record RequesterResponse(Long userId, String nickname, String email) {
        public static RequesterResponse from(User user) {
            return new RequesterResponse(user.getUserId(), user.getNickname(), user.getEmail());
        }
    }
}
