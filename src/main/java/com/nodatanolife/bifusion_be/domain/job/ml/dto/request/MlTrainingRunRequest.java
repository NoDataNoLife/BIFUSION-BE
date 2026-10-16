package com.nodatanolife.bifusion_be.domain.job.ml.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nodatanolife.bifusion_be.domain.job.entity.TrainingParameters;

/**
 * ML POST /api/v1/training/run 요청 본문.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MlTrainingRunRequest(
        String datasetId,
        Integer kShot,
        Integer epochs,
        Integer qQuery,
        Integer episodesPerEpoch,
        Long seed
) {

    public static MlTrainingRunRequest from(String mlDatasetId, TrainingParameters parameters) {
        return new MlTrainingRunRequest(
                mlDatasetId,
                parameters.kShot(),
                parameters.epochs(),
                parameters.qQuery(),
                parameters.episodesPerEpoch(),
                parameters.seed()
        );
    }
}
