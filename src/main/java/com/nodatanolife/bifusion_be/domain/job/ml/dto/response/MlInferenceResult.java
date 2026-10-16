package com.nodatanolife.bifusion_be.domain.job.ml.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nodatanolife.bifusion_be.domain.job.entity.PredictedClass;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ML 추론 결과(result). 모든 필드 nullable.
 * predictions[].sample_index는 요청에 보낸 이미지 순서다.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MlInferenceResult(
        List<Prediction> predictions,
        List<String> imageKeys
) {

    /**
     * SUCCESS 응답인데 결과로 저장할 수 없으면 그 사유. 정상이면 null.
     * predictions는 입력 이미지마다 하나씩, sample_index는 0 이상 입력 수 미만이고 중복이 없어야 한다.
     */
    public String invalidReason(int inputCount) {
        if (predictions == null || predictions.size() != inputCount) {
            return "predictions " + (predictions == null ? 0 : predictions.size()) + "건 / 입력 " + inputCount + "건";
        }
        Set<Integer> seen = new HashSet<>();
        for (Prediction prediction : predictions) {
            Integer index = prediction.sampleIndex();
            if (index == null || index < 0 || index >= inputCount || !seen.add(index)) {
                return "sample_index 범위 밖 또는 중복: " + index;
            }
            if (prediction.predictedClass() == null) {
                return "predicted_class 없음: sample_index " + index;
            }
        }
        return null;
    }

    /**
     * 추론 요약 (정상·비정상 개수, 평균 신뢰도). invalidReason()이 null인 결과에만 호출한다.
     */
    public Summary summary() {
        int normalCount = (int) predictions.stream()
                .filter(p -> p.predictedClass() == PredictedClass.NORMAL)
                .count();
        BigDecimal confidenceSum = predictions.stream()
                .map(p -> p.confidence() != null ? p.confidence() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgConfidence = predictions.isEmpty()
                ? null
                : confidenceSum.divide(BigDecimal.valueOf(predictions.size()), 4, RoundingMode.HALF_UP);
        return new Summary(normalCount, predictions.size() - normalCount, avgConfidence);
    }

    public record Summary(
            int normalCount,
            int anomalyCount,
            BigDecimal avgConfidence
    ) {
    }

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
