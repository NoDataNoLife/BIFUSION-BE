package com.nodatanolife.bifusion_be.domain.job.ml.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * ML 증강 결과(result). 모든 필드 nullable.
 * 결과 파일 위치는 artifacts의 bucket/key만 사용한다 (dataset_url·preview_url은 ML이 만든 임시 주소라 쓰지 않는다).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MlAugmentationResult(
        String datasetId,
        Integer numNormal,
        Integer numAnomaly,
        Integer totalSamples,
        Integer numSyntheticGenerated,
        MlArtifacts artifacts
) {

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

    public MlArtifact download() {
        return artifacts != null ? artifacts.download() : null;
    }

    public MlArtifact preview() {
        return artifacts != null ? artifacts.preview() : null;
    }
}
