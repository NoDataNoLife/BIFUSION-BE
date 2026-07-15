package com.nodatanolife.bifusion_be.domain.recruitment.dto.response;

import com.nodatanolife.bifusion_be.domain.recruitment.entity.RecruitApplication;

import java.time.LocalDateTime;

public record RecruitApplicationStatusResponse(
        Long recruitApplicationId,
        Long recruitId,
        String applicantNickname,
        String status,
        LocalDateTime updatedAt
) {
    public static RecruitApplicationStatusResponse from(RecruitApplication application) {
        return new RecruitApplicationStatusResponse(
                application.getRecruitApplicationId(),
                application.getRecruit().getRecruitId(),
                application.getUser().getNickname(),
                application.getStatus().name(),
                application.getUpdatedAt()
        );
    }
}