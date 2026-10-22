package com.nodatanolife.bifusion_be.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * 대시보드 리소스 사용량 (스토리지·월간 증강 카드).
 * 사용량 값이 null이면 집계에 실패한 것이고, 0이면 실제로 사용량이 없는 것이다.
 * 한도(storageLimitBytes, monthlyAugmentationLimit)는 정해지지 않아 null로 내려준다 (필드는 항상 포함).
 */
@Schema(description = "리소스 사용량")
public record ResourceUsageResponse(
        @Schema(description = "사용 중인 저장 용량(byte). 집계에 실패하면 null", example = "52428800")
        Long storageUsedBytes,
        @Schema(description = "저장 용량 한도(byte). 한도가 정해지지 않아 현재는 항상 null")
        Long storageLimitBytes,
        @Schema(description = "크기 정보가 없어 storageUsedBytes에서 빠진 파일 수. 0이 아니면 실제 사용량이 조금 더 클 수 있다. 집계에 실패하면 null", example = "0")
        Long storageUnmeasuredFileCount,
        @Schema(description = "이번 달 증강 작업 횟수 (실패·취소 제외, 진행 중 포함). 집계에 실패하면 null", example = "3")
        Long monthlyAugmentations,
        @Schema(description = "월간 증강 한도. 횟수 제한이 없어 현재는 항상 null (작업 비용은 토큰으로 차감)")
        Long monthlyAugmentationLimit,
        @Schema(description = "집계 기간 시작 (포함)", example = "2026-10-01T00:00:00+09:00")
        OffsetDateTime periodStart,
        @Schema(description = "집계 기간 끝 (미포함, 다음 달 1일 0시)", example = "2026-11-01T00:00:00+09:00")
        OffsetDateTime periodEnd,
        @Schema(description = "집계 기간의 타임존", example = "Asia/Seoul")
        String timezone
) {
}
