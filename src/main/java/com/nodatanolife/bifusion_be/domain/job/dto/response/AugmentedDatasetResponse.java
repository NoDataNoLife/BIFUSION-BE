package com.nodatanolife.bifusion_be.domain.job.dto.response;

import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionTargetResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationConfig;

import java.time.LocalDateTime;

/**
 * 학습에 쓸 수 있는 증강 결과 (학습 화면의 증강 데이터 선택 목록).
 *
 * @param datasetId 증강 jobId. 학습 요청의 dataset_id로 그대로 사용한다.
 */
@Schema(description = "학습에 쓸 수 있는 증강 데이터")
public record AugmentedDatasetResponse(
        @Schema(description = "학습 요청의 dataset_id로 그대로 보내는 값 (= 증강 작업 번호)", example = "15")
        Long datasetId,
        @Schema(description = "화면 표시용 코드", example = "JOB-015")
        String jobCode,
        @Schema(description = "전체 이미지 수 (원본 + 생성분)", example = "40")
        Integer totalSampleCount,
        @Schema(description = "새로 만든 이미지 수", example = "30")
        Integer generatedCount,
        @Schema(description = "처음 올린 정상 이미지 수", example = "5")
        Integer normalCount,
        @Schema(description = "처음 올린 비정상 이미지 수", example = "5")
        Integer anomalyCount,
        @Schema(description = "미리보기 이미지 주소 (presigned, 없으면 null)")
        @PresignedUrl String previewUrl,
        @Schema(description = "증강이 끝난 시각")
        LocalDateTime finishedAt,
        @Schema(description = "검수 신청 대상 정보. 검수 신청 시 inspectionTarget.targetId를 보낸다 (datasetId와 다름). 검수 대상이 아니면 null")
        InspectionTargetResponse inspectionTarget
) {

    public static AugmentedDatasetResponse from(AugmentationConfig config, InspectionTargetResponse inspectionTarget) {
        return new AugmentedDatasetResponse(
                config.getJob().getJobId(),
                config.getJob().getDisplayCode(),
                config.getTotalSampleCount(),
                config.getGeneratedCount(),
                config.getNormalCount(),
                config.getAnomalyCount(),
                config.getPreviewKey(),
                config.getJob().getFinishedAt(),
                inspectionTarget
        );
    }
}
