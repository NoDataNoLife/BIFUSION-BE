package com.nodatanolife.bifusion_be.domain.recruitment.dto.response;

import com.nodatanolife.bifusion_be.domain.recruitment.entity.RecruitApplication;

import java.time.LocalDateTime;

public record RecruitApplyResponse(
        Long applicationId,
        LocalDateTime createdAt
) {
    public static RecruitApplyResponse from(RecruitApplication application) {
        return new RecruitApplyResponse(
                application.getRecruitApplicationId(),
                application.getCreatedAt()
        );
    }
}