package com.nodatanolife.bifusion_be.domain.job.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
import com.nodatanolife.bifusion_be.domain.job.entity.TrainedModel;
import com.nodatanolife.bifusion_be.domain.job.entity.TrainingConfig;

import java.math.BigDecimal;

/**
 * @param trainedModelId 추론 요청의 model_id로 쓰는 값
 */
@Schema(description = "학습 결과")
public record TrainingJobResult(
        @Schema(description = "추론 요청의 model_id로 쓰는 값", example = "3")
        Long trainedModelId,
        @Schema(description = "학습에 쓴 K-shot", example = "5")
        Integer kShot,
        @Schema(description = "학습 성능")
        Metrics metrics,
        @Schema(description = "모델 파일 다운로드 주소 (presigned, 없으면 null)")
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

    @Schema(name = "TrainingJobMetrics", description = "학습 성능 지표")
    public record Metrics(
            @Schema(description = "정확도 (0~1)", example = "0.9123")
            BigDecimal accuracy,
            @Schema(description = "AUROC (0~1)", example = "0.95")
            BigDecimal auroc,
            @Schema(description = "손실", example = "0.1234")
            BigDecimal loss
    ) {
    }
}
