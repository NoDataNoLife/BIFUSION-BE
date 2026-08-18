package com.nodatanolife.bifusion_be.domain.expert.dto.response;

import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertTask;
import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertVerification;
import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertVerificationStatus;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationConfig;
import com.nodatanolife.bifusion_be.domain.user.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExpertTaskSummaryResponse(
        String reviewCode,
        String status,
        ExpertVerificationStatus reviewResult,
        RequesterResponse requester,
        String projectName,
        LocalDate requestedAt,
        LocalDateTime updatedAt
) {
    public static ExpertTaskSummaryResponse of(ExpertTask task, AugmentationConfig config) {
        ExpertVerification verification = task.getExpertVerification();
        return new ExpertTaskSummaryResponse(
                task.getReviewCode(),
                task.getStatus(),
                verification.getStatus() == ExpertVerificationStatus.PENDING ? null : verification.getStatus(),
                RequesterResponse.from(verification.getUser()),
                config.getJob().getProject().getTitle(),
                verification.getAppliedAt() != null ? verification.getAppliedAt().toLocalDate() : null,
                task.getUpdatedAt()
        );
    }

    public record RequesterResponse(Long userId, String nickname, String email) {
        public static RequesterResponse from(User user) {
            return new RequesterResponse(user.getUserId(), user.getNickname(), user.getEmail());
        }
    }
}
