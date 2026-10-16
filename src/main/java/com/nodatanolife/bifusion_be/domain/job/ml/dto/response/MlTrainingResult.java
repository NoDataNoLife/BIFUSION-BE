package com.nodatanolife.bifusion_be.domain.job.ml.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;

/**
 * ML 학습 결과(result). 모든 필드 nullable.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MlTrainingResult(
        String modelId,
        String modelUrl,
        Metrics metrics,
        String modelKey,
        String bucket
) {

    /**
     * @param ourBucket 우리 S3 버킷명 (spring.cloud.aws.s3.bucket). 다른 버킷의 파일이면 null.
     */
    public String modelStorageKey(String ourBucket) {
        return MlStorageKeys.resolve(modelKey, modelUrl, bucket, ourBucket);
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Metrics(
            BigDecimal accuracy,
            BigDecimal auroc,
            BigDecimal loss
    ) {
    }
}
