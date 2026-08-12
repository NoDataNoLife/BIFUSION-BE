package com.nodatanolife.bifusion_be.domain.expert.dto.response;

import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertVerification;
import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertVerificationStatus;

import java.time.LocalDateTime;

public record InspectionResponse(
        Long inspectionId,
        ExpertVerificationStatus status,
        LocalDateTime createdAt
) {
    public static InspectionResponse from(ExpertVerification verification) {
        return new InspectionResponse(
                verification.getExpertVerificationId(),
                verification.getStatus(),
                verification.getCreatedAt()
        );
    }
}
