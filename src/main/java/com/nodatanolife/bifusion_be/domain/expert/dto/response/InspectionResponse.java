package com.nodatanolife.bifusion_be.domain.expert.dto.response;

import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequest;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequestStatus;

import java.time.LocalDateTime;

public record InspectionResponse(
        Long inspectionId,
        InspectionRequestStatus status,
        LocalDateTime createdAt
) {
    public static InspectionResponse from(InspectionRequest verification) {
        return new InspectionResponse(
                verification.getInspectionRequestId(),
                verification.getStatus(),
                verification.getCreatedAt()
        );
    }
}
