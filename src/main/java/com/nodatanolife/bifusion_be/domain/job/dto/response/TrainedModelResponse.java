package com.nodatanolife.bifusion_be.domain.job.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "추론에 쓸 수 있는 학습 모델")
public record TrainedModelResponse(
        @Schema(description = "추론 요청의 model_id로 그대로 보내는 값", example = "3")
        Long trainedModelId,
        @Schema(description = "이 모델을 만든 학습 작업의 코드", example = "JOB-016")
        String jobCode,
        @Schema(description = "학습에 쓴 K-shot", example = "5")
        Integer kShot,
        @Schema(description = "학습 성능")
        Metrics metrics,
        @Schema(description = "학습에 쓴 증강 데이터 번호", example = "15")
        Long sourceDatasetId,
        @Schema(description = "학습에 쓴 증강 데이터 코드", example = "JOB-015")
        String sourceDatasetJobCode,
        @Schema(description = "학습이 끝난 시각")
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
                config != null ? new Metrics(config.getAccuracy(), config.getF1Score(), config.getAuroc(), config.getLoss()) : null,
                sourceJob != null ? sourceJob.getJobId() : null,
                sourceJob != null ? sourceJob.getDisplayCode() : null,
                trainingJob.getFinishedAt()
        );
    }

    @Schema(name = "TrainedModelMetrics", description = "학습 성능 지표")
    public record Metrics(
            @Schema(description = "정확도 (0~1)", example = "0.9123")
            BigDecimal accuracy,
            @Schema(description = "F1 점수 (0~1)", example = "0.9")
            BigDecimal f1Score,
            @Schema(description = "AUROC (0~1)", example = "0.95")
            BigDecimal auroc,
            @Schema(description = "손실", example = "0.1234")
            BigDecimal loss
    ) {
    }
}
