package com.nodatanolife.bifusion_be.domain.expert.dto.response;

import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertTask;

public record ExpertTaskStartResponse(
        Long taskId,
        String status
) {
    public static ExpertTaskStartResponse from(ExpertTask task) {
        return new ExpertTaskStartResponse(
                task.getExpertTaskId(),
                task.getStatus()
        );
    }
}
