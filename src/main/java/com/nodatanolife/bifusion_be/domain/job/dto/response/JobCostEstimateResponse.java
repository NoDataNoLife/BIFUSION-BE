package com.nodatanolife.bifusion_be.domain.job.dto.response;

import com.nodatanolife.bifusion_be.domain.job.entity.JobType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 작업 예상 비용 (실행 시 차감되는 앤트)")
public record JobCostEstimateResponse(
        @Schema(description = "작업 종류", example = "AUGMENTATION")
        JobType type,
        @Schema(description = "실행하면 차감되는 앤트. 실패·취소되면 전액 돌려받습니다", example = "50")
        int cost,
        @Schema(description = "현재 보유 앤트", example = "500")
        int currentPoints,
        @Schema(description = "보유 앤트로 실행할 수 있는지 (currentPoints >= cost)", example = "true")
        boolean affordable
) {

    public static JobCostEstimateResponse of(JobType type, int cost, int currentPoints) {
        return new JobCostEstimateResponse(type, cost, currentPoints, currentPoints >= cost);
    }
}
