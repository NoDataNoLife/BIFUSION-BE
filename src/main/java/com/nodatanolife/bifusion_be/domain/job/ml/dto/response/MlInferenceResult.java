package com.nodatanolife.bifusion_be.domain.job.ml.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nodatanolife.bifusion_be.domain.job.entity.PredictedClass;

import java.math.BigDecimal;
import java.util.List;

/**
 * ML 추론 결과(result). 모든 필드 nullable.
 * predictions[].sample_index는 요청에 보낸 이미지 순서다.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MlInferenceResult(
        List<Prediction> predictions,
        List<String> imageKeys
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Prediction(
            Integer sampleIndex,
            PredictedClass predictedClass,
            BigDecimal confidence,
            Probabilities probabilities
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Probabilities(
            BigDecimal normal,
            BigDecimal anomaly
    ) {
    }
}
