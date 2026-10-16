package com.nodatanolife.bifusion_be.domain.job.dto.response;

import com.nodatanolife.bifusion_be.domain.job.entity.Job;
import com.nodatanolife.bifusion_be.domain.job.entity.TrainedModel;
import com.nodatanolife.bifusion_be.domain.job.entity.TrainingConfig;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 추론에 쓸 수 있는 학습 모델 (추론 화면의 모델 선택 목록).
 *
 * @param trainedModelId 추론 요청의 model_id로 그대로 사용한다.
 * @param sourceDatasetId 원본 증강 jobId
 */
public record TrainedModelResponse(
        Long trainedModelId,
        String jobCode,
        Integer kShot,
        Metrics metrics,
        Long sourceDatasetId,
        String sourceDatasetJobCode,
        LocalDateTime finishedAt
) {

    /**
     * @param config 학습 설정. 없으면(데이터 불일치) 지표·원본 정보는 null로 둔다.
     */
    public static TrainedModelResponse from(TrainedModel model, TrainingConfig config) {
        Job trainingJob = model.getTrainingJob();
        Job sourceJob = config != null ? config.getAugmentationConfig().getJob() : null;
        return new TrainedModelResponse(
                model.getTrainedModelId(),
                trainingJob.getDisplayCode(),
                model.getKShot(),
                config != null ? new Metrics(config.getAccuracy(), config.getAuroc(), config.getLoss()) : null,
                sourceJob != null ? sourceJob.getJobId() : null,
                sourceJob != null ? sourceJob.getDisplayCode() : null,
                trainingJob.getFinishedAt()
        );
    }

    public record Metrics(
            BigDecimal accuracy,
            BigDecimal auroc,
            BigDecimal loss
    ) {
    }
}
