package com.nodatanolife.bifusion_be.domain.job.dto.response;

import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationConfig;

/**
 * @param datasetId 학습 요청의 dataset_id로 쓰는 값 (= 이 증강 jobId)
 */
public record AugmentationJobResult(
        Long datasetId,
        Integer generatedCount,
        Integer totalSampleCount,
        @PresignedUrl String previewUrl,
        @PresignedUrl String downloadUrl
) implements JobResult {

    public static AugmentationJobResult from(AugmentationConfig config) {
        return new AugmentationJobResult(
                config.getJob().getJobId(),
                config.getGeneratedCount(),
                config.getTotalSampleCount(),
                config.getPreviewKey(),
                config.getDatasetKey()
        );
    }
}
