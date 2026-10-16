package com.nodatanolife.bifusion_be.domain.job.ml.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * ML 증강 결과(result). 모든 필드 nullable.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MlAugmentationResult(
        String datasetId,
        String datasetUrl,
        String previewUrl,
        Integer numNormal,
        Integer numAnomaly,
        Integer totalSamples,
        Integer numSyntheticGenerated,
        String datasetKey,
        String previewKey,
        String bucket
) {

    /**
     * @param ourBucket 우리 S3 버킷명 (spring.cloud.aws.s3.bucket). 다른 버킷의 파일이면 null.
     */
    /**
     * SUCCESS 응답인데 결과로 저장할 수 없으면 그 사유. 정상이면 null.
     * (응답 본문 대신 이 사유를 mlErrorDetail에 남긴다)
     */
    public String invalidReason() {
        if (datasetId == null || datasetId.isBlank()) {
            return "SUCCESS 응답에 dataset_id 없음";
        }
        return null;
    }

    public String datasetStorageKey(String ourBucket) {
        return MlStorageKeys.resolve(datasetKey, datasetUrl, bucket, ourBucket);
    }

    /**
     * @param ourBucket 우리 S3 버킷명 (spring.cloud.aws.s3.bucket). 다른 버킷의 파일이면 null.
     */
    public String previewStorageKey(String ourBucket) {
        return MlStorageKeys.resolve(previewKey, previewUrl, bucket, ourBucket);
    }
}
