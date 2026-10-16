package com.nodatanolife.bifusion_be.domain.job.dto.response;

import com.nodatanolife.bifusion_be.domain.job.entity.InferenceResult;
import com.nodatanolife.bifusion_be.domain.job.entity.PredictedClass;

import java.math.BigDecimal;
import java.util.List;

public record InferenceJobResult(
        List<Prediction> predictions
) implements JobResult {

    public static InferenceJobResult from(List<InferenceResult> results) {
        return new InferenceJobResult(results.stream().map(Prediction::from).toList());
    }

    public record Prediction(
            Long fileId,
            Integer sampleIndex,
            PredictedClass predictedClass,
            BigDecimal confidence,
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

    public record Probabilities(
            BigDecimal normal,
            BigDecimal anomaly
    ) {
    }
}
