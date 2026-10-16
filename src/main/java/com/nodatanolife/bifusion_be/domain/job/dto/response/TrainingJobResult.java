package com.nodatanolife.bifusion_be.domain.job.dto.response;

import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
import com.nodatanolife.bifusion_be.domain.job.entity.TrainedModel;
import com.nodatanolife.bifusion_be.domain.job.entity.TrainingConfig;

import java.math.BigDecimal;

/**
 * @param trainedModelId 추론 요청의 model_id로 쓰는 값
 */
public record TrainingJobResult(
        Long trainedModelId,
        Integer kShot,
        Metrics metrics,
        @PresignedUrl String downloadUrl
) implements JobResult {

    public static TrainingJobResult from(TrainingConfig config, TrainedModel model) {
        return new TrainingJobResult(
                model != null ? model.getTrainedModelId() : null,
                config.getKShot(),
                new Metrics(config.getAccuracy(), config.getAuroc(), config.getLoss()),
                model != null ? model.getModelKey() : null
        );
    }

    public record Metrics(
            BigDecimal accuracy,
            BigDecimal auroc,
            BigDecimal loss
    ) {
    }
}
