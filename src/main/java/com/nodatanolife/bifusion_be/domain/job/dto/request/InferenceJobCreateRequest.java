package com.nodatanolife.bifusion_be.domain.job.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InferenceJobCreateRequest extends JobCreateRequest {

    // BE trainedModelId (학습 결과의 trainedModelId)
    @NotNull(message = "추론에 사용할 모델을 선택해 주세요.")
    @JsonProperty("model_id")
    private Long modelId;

    @NotNull(message = "분류할 이미지를 선택해 주세요.")
    private List<Long> fileIds;
}
