package com.nodatanolife.bifusion_be.domain.expert.dto.response;

import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequest;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionStatus;

/**
 * Recipe/Dataset 상세 응답이 노출하는 검증 상태 + 결과 묶음.
 * InspectionRequest.status를 조회 시점에 파생시켜 만들며, Recipe/Dataset에는 별도로 저장하지 않는다.
 */
public record InspectionSummary(
        InspectionStatus status,
        InspectionResultResponse result
) {
    private static final InspectionSummary NONE = new InspectionSummary(InspectionStatus.NONE, null);

    public static InspectionSummary none() {
        return NONE;
    }

    public static InspectionSummary of(InspectionRequest inspectionRequest) {
        return switch (inspectionRequest.getStatus()) {
            case PENDING -> new InspectionSummary(InspectionStatus.PENDING, null);
            case APPROVED -> new InspectionSummary(InspectionStatus.COMPLETED, InspectionResultResponse.from(inspectionRequest));
            case REJECTED -> NONE;
        };
    }
}
