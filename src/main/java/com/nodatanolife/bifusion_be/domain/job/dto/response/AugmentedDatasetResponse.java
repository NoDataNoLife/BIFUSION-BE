package com.nodatanolife.bifusion_be.domain.job.dto.response;

import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationConfig;

import java.time.LocalDateTime;

/**
 * 학습에 쓸 수 있는 증강 결과 (학습 화면의 증강 데이터 선택 목록).
 *
 * @param datasetId 증강 jobId. 학습 요청의 dataset_id로 그대로 사용한다.
 */
public record AugmentedDatasetResponse(
        Long datasetId,
        String jobCode,
        Integer totalSampleCount,
        Integer generatedCount,
        Integer normalCount,
        Integer anomalyCount,
        @PresignedUrl String previewUrl,
        LocalDateTime finishedAt
) {

    public static AugmentedDatasetResponse from(AugmentationConfig config) {
        return new AugmentedDatasetResponse(
                config.getJob().getJobId(),
                config.getJob().getDisplayCode(),
                config.getTotalSampleCount(),
                config.getGeneratedCount(),
                config.getNormalCount(),
                config.getAnomalyCount(),
                config.getPreviewKey(),
                config.getJob().getFinishedAt()
        );
    }
}
