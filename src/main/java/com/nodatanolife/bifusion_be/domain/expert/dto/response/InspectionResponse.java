package com.nodatanolife.bifusion_be.domain.expert.dto.response;

import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequest;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequestStatus;

import java.time.LocalDateTime;

/**
 * 검수 신청·취소 응답 (신청자용).
 *
 * @param inspectionRequestId 검수 신청 ID. 취소 API에 사용한다. 전문가용 /inspections/{inspectionId}의 ID(배정 task ID)와 다르다.
 */
public record InspectionResponse(
        Long inspectionRequestId,
        InspectionRequestStatus status,
        LocalDateTime createdAt
) {
    public static InspectionResponse from(InspectionRequest inspectionRequest) {
        return new InspectionResponse(
                inspectionRequest.getInspectionRequestId(),
                inspectionRequest.getStatus(),
                inspectionRequest.getCreatedAt()
        );
    }
}
