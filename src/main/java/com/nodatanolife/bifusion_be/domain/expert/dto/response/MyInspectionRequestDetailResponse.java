package com.nodatanolife.bifusion_be.domain.expert.dto.response;

import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionProgressStatus;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequest;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * "내가 요청한 검수" 상세 (신청자용). 목록 항목 정보에 신청 내용(요청 메모·리워드)을 더한다.
 */
public record MyInspectionRequestDetailResponse(
        Long inspectionRequestId,
        MyInspectionRequestResponse.Target target,
        InspectionProgressStatus status,
        LocalDateTime requestedAt,
        LocalDateTime completedAt,
        MyInspectionRequestResponse.Expert expert,
        MyInspectionRequestResponse.Result result,
        @Schema(description = "신청 시 남긴 검증 요청 메모")
        String reason,
        @Schema(description = "신청 시 건 리워드 포인트")
        Integer rewardPoints
) {
    public static MyInspectionRequestDetailResponse of(InspectionRequest inspectionRequest, MyInspectionRequestResponse summary) {
        return new MyInspectionRequestDetailResponse(
                summary.inspectionRequestId(),
                summary.target(),
                summary.status(),
                summary.requestedAt(),
                summary.completedAt(),
                summary.expert(),
                summary.result(),
                inspectionRequest.getReason(),
                inspectionRequest.getRewardPoints()
        );
    }
}
