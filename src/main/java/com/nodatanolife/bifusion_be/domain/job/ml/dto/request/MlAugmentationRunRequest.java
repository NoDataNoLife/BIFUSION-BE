package com.nodatanolife.bifusion_be.domain.job.ml.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationParameters;

import java.util.List;

/**
 * ML POST /api/v1/augmentation/run 요청 본문.
 * BE가 외부로 보내는 요청이라 record로 생성한다. 이미지 본문이 담기므로 toString/로그 출력 금지.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MlAugmentationRunRequest(
        Integer numSamplesPerClass,
        Double strength,
        Integer finetuneSteps,
        Integer steps,
        Long seed,
        List<String> normalImagesBase64,
        List<String> anomalyImagesBase64
) {

    public static MlAugmentationRunRequest from(AugmentationParameters parameters,
                                                List<String> normalImagesBase64,
                                                List<String> anomalyImagesBase64) {
        return new MlAugmentationRunRequest(
                parameters.numSamplesPerClass(),
                parameters.strength(),
                parameters.finetuneSteps(),
                parameters.steps(),
                parameters.seed(),
                normalImagesBase64,
                anomalyImagesBase64
        );
    }

    @Override
    public String toString() {
        return "MlAugmentationRunRequest[normal=" + normalImagesBase64.size()
                + ", anomaly=" + anomalyImagesBase64.size() + "]";
    }
}
