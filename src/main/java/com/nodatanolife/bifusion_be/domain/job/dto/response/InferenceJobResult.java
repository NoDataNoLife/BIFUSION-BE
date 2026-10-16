package com.nodatanolife.bifusion_be.domain.job.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.nodatanolife.bifusion_be.domain.job.entity.InferenceResult;
import com.nodatanolife.bifusion_be.domain.job.entity.PredictedClass;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "추론 결과")
public record InferenceJobResult(
        @Schema(description = "이미지별 분류 결과 (요청한 fileIds 순서)")
        List<Prediction> predictions
) implements JobResult {

    public static InferenceJobResult from(List<InferenceResult> results) {
        return new InferenceJobResult(results.stream().map(Prediction::from).toList());
    }

    @Schema(name = "InferencePrediction", description = "이미지 1장의 분류 결과")
    public record Prediction(
            @Schema(description = "분류한 이미지의 fileId", example = "301")
            Long fileId,
            @Schema(description = "요청 순서 (0부터)", example = "0")
            Integer sampleIndex,
            @Schema(description = "예측 클래스", example = "ANOMALY")
            PredictedClass predictedClass,
            @Schema(description = "예측 신뢰도 (0~1)", example = "0.87")
            BigDecimal confidence,
            @Schema(description = "클래스별 확률")
            Probabilities probabilities
    ) {

        public static Prediction from(InferenceResult result) {
            return new Prediction(
                    result.getFile().getFileId(),
                    result.getSampleIndex(),
                    result.getPredictedClass(),
                    result.getConfidence(),
                    new Probabilities(result.getNormalProbability(), result.getAnomalyProbability())
            );
        }
    }

    @Schema(name = "InferenceProbabilities", description = "클래스별 확률")
    public record Probabilities(
            @Schema(description = "정상 확률", example = "0.13")
            BigDecimal normal,
            @Schema(description = "비정상 확률", example = "0.87")
            BigDecimal anomaly
    ) {
    }
}
