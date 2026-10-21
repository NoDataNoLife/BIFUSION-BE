package com.nodatanolife.bifusion_be.domain.job.ml.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;

/**
 * ML 학습 결과(result). 모든 필드 nullable.
 * 모델 파일 위치는 artifacts.download의 bucket/key만 사용한다 (model_url은 ML 로컬 경로라 쓰지 않는다).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MlTrainingResult(
        String modelId,
        Metrics metrics,
        MlArtifacts artifacts
) {

    /**
     * SUCCESS 응답인데 결과로 저장할 수 없으면 그 사유. 정상이면 null.
     */
    public String invalidReason() {
        if (modelId == null || modelId.isBlank()) {
            return "SUCCESS 응답에 model_id 없음";
        }
        return null;
    }

    public MlArtifact download() {
        return artifacts != null ? artifacts.download() : null;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Metrics(
            BigDecimal accuracy,
            BigDecimal auroc,
            BigDecimal loss
    ) {
    }
}
