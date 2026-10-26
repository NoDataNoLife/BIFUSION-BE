package com.nodatanolife.bifusion_be.domain.expert.dto.response;

import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertTask;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionProgressStatus;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequest;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;
import com.nodatanolife.bifusion_be.domain.expert.service.InspectionTargetDisplayResolver.TargetDisplay;
import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * "내가 요청한 검수" 목록 항목 (신청자용).
 */
public record MyInspectionRequestResponse(
        @Schema(description = "검수 신청 ID. 상세·취소 API에 사용. 전문가용 /inspections/{inspectionId}의 ID(배정 ID)와 다르다", example = "45")
        Long inspectionRequestId,
        Target target,
        @Schema(description = "진행 상태 (PENDING/IN_PROGRESS/APPROVED/REJECTED)")
        InspectionProgressStatus status,
        @Schema(description = "신청 시각 (재신청했으면 마지막 신청 시각)")
        LocalDateTime requestedAt,
        @Schema(description = "승인·반려 시각. 확정 전이면 null")
        LocalDateTime completedAt,
        @Schema(description = "담당 전문가. 배정 전이면 null")
        Expert expert,
        @Schema(description = "전문가 최종 의견(승인) 또는 반려 사유. 확정 전이면 null")
        Result result
) {

    /**
     * @param id    검수 신청 API의 targetId (증강 데이터는 AugmentationConfig ID)
     * @param jobId 증강 데이터 화면 이동용. 레시피·데이터셋은 null
     * @param name  레시피·데이터셋 제목, 증강 데이터는 프로젝트명. 대상을 찾지 못하면 "알 수 없는 대상"
     * @param imageUrl 레시피 썸네일, 데이터셋 배너, 증강 데이터 미리보기. 없으면 null
     * @param deleted 대상이 삭제됐으면 true
     */
    public record Target(
            InspectionTargetType type,
            Long id,
            Long jobId,
            String name,
            @PresignedUrl String imageUrl,
            boolean deleted
    ) {
        private static final String UNKNOWN_NAME = "알 수 없는 대상";

        /**
         * @param display 대상을 찾지 못했으면 null ("알 수 없는 대상", 삭제된 것으로 표시)
         */
        public static Target of(InspectionRequest inspectionRequest, TargetDisplay display) {
            if (display == null) {
                return new Target(inspectionRequest.getTargetType(), inspectionRequest.getTargetId(), null,
                        UNKNOWN_NAME, null, true);
            }
            return new Target(inspectionRequest.getTargetType(), inspectionRequest.getTargetId(), display.jobId(),
                    display.name(), display.imageUrl(), display.deleted());
        }
    }

    public record Expert(String nickname) {
    }

    public record Result(String comment) {
    }

    /**
     * @param latestTask 현재 회차 task. 배정되지 않았으면 null
     */
    public static MyInspectionRequestResponse of(InspectionRequest inspectionRequest, ExpertTask latestTask, Target target) {
        InspectionProgressStatus status = InspectionProgressStatus.of(
                inspectionRequest.getStatus(), latestTask != null ? latestTask.getStatus() : null);
        boolean decided = status == InspectionProgressStatus.APPROVED || status == InspectionProgressStatus.REJECTED;
        return new MyInspectionRequestResponse(
                inspectionRequest.getInspectionRequestId(),
                target,
                status,
                inspectionRequest.getAppliedAt(),
                decided ? inspectionRequest.getReviewedAt() : null,
                latestTask != null ? new Expert(latestTask.getExpert().getNickname()) : null,
                decided ? new Result(inspectionRequest.getComment()) : null
        );
    }
}
