package com.nodatanolife.bifusion_be.domain.expert.dto.response;

import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertApplication;

import java.time.LocalDateTime;

public record ExpertApplicationResponse(
        String expertStatus,
        LocalDateTime createdAt
) {
    public static ExpertApplicationResponse from(ExpertApplication application) {
        return new ExpertApplicationResponse(
                application.getStatus(),
                application.getCreatedAt()
        );
    }
}
