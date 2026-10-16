package com.nodatanolife.bifusion_be.domain.job.ml.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

/**
 * ML POST /api/v1/inference/run 요청 본문.
 * model_id를 생략하면 ML이 기본 모델(best.pt)을 쓰므로 항상 채워 보낸다. 이미지 본문이 담기므로 toString/로그 출력 금지.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MlInferenceRunRequest(
        String modelId,
        List<String> imagesBase64
) {

    public static MlInferenceRunRequest from(String mlModelId, List<String> imagesBase64) {
        return new MlInferenceRunRequest(mlModelId, imagesBase64);
    }

    @Override
    public String toString() {
        return "MlInferenceRunRequest[modelId=" + modelId + ", images=" + imagesBase64.size() + "]";
    }
}
