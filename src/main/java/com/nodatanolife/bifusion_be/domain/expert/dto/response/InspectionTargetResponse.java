package com.nodatanolife.bifusion_be.domain.expert.dto.response;

import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequest;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequestStatus;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 화면의 항목을 검수 신청 API(POST /api/v1/inspections)와 잇는 정보.
 * 화면은 자기 ID(예: 증강 데이터셋의 jobId)로 항목을 식별하고, 검수 신청 때만 targetType·targetId를 그대로 보낸다.
 */
@Schema(description = "검수 신청 대상 정보")
public record InspectionTargetResponse(
        @Schema(description = "검수 신청 API의 targetType", example = "AUGMENTED_DATA")
        InspectionTargetType targetType,
        @Schema(description = "검수 신청 API의 targetId (증강 데이터는 AugmentationConfig ID)", example = "123")
        Long targetId,
        @Schema(description = "가장 최근 검수 신청 ID (취소 API에 사용). 신청 이력이 없으면 null", example = "45")
        Long inspectionRequestId,
        @Schema(description = "지금 검수를 신청할 수 있는지. 본인 소유이고 진행 중·승인된 신청이 없을 때 true (반려·취소 후 재신청 가능). 포인트는 신청 시 검증")
        boolean canRequest,
        @Schema(description = "가장 최근 신청 상태 (PENDING/APPROVED/REJECTED/CANCELLED). 신청 이력이 없으면 null")
        InspectionRequestStatus status,
        @Schema(description = "승인·반려 결과 (검수자·점수·코멘트·처리 시각). 처리 전이거나 신청 이력이 없으면 null")
        InspectionResultResponse result,
        @Schema(description = "신청할 수 있는 최소 리워드. 대상의 스토리지 크기(MB)에 비례", example = "100")
        int minRewardPoints,
        @Schema(description = "신청할 수 있는 최대 리워드. 대상의 스토리지 크기(MB)에 비례", example = "1000")
        int maxRewardPoints
) {

    /**
     * @param latest 대상의 가장 최근 신청. 없으면 null
     */
    public static InspectionTargetResponse of(InspectionTargetType targetType, Long targetId, boolean canRequest,
                                              InspectionRequest latest, int minRewardPoints, int maxRewardPoints) {
        if (latest == null) {
            return new InspectionTargetResponse(targetType, targetId, null, canRequest, null, null,
                    minRewardPoints, maxRewardPoints);
        }
        InspectionRequestStatus status = latest.getStatus();
        boolean processed = status == InspectionRequestStatus.APPROVED || status == InspectionRequestStatus.REJECTED;
        return new InspectionTargetResponse(targetType, targetId, latest.getInspectionRequestId(), canRequest, status,
                processed ? InspectionResultResponse.from(latest) : null, minRewardPoints, maxRewardPoints);
    }
}
