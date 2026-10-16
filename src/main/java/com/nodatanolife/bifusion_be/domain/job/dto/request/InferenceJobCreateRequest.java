package com.nodatanolife.bifusion_be.domain.job.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "추론 실행 요청 (type=INFERENCE)")
public class InferenceJobCreateRequest extends JobCreateRequest {

    // BE trainedModelId (학습 결과의 trainedModelId)
    @Schema(description = "추론에 쓸 모델 (학습 모델 목록의 trainedModelId)",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    @NotNull(message = "추론에 사용할 모델을 선택해 주세요.")
    @JsonProperty("model_id")
    private Long modelId;

    @Schema(description = "분류할 이미지 fileId 목록 (/api/v1/files/temp 업로드 결과, 1~20장). 결과는 이 순서대로 나옵니다.",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "[301, 302]")
    @NotNull(message = "분류할 이미지를 선택해 주세요.")
    private List<Long> fileIds;
}
