package com.nodatanolife.bifusion_be.domain.job.dto.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.nodatanolife.bifusion_be.domain.job.entity.JobType;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI Job 생성 요청. body의 type 값으로 하위 타입을 결정한다.
 * FE가 기존에 보내는 필드명(snake_case 포함)을 그대로 받는다. 범위 검증은 yml 값이 필요해 JobRequestValidator에서 한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AugmentationJobCreateRequest.class, name = "AUGMENTATION"),
        @JsonSubTypes.Type(value = TrainingJobCreateRequest.class, name = "TRAINING"),
        @JsonSubTypes.Type(value = InferenceJobCreateRequest.class, name = "INFERENCE")
})
public abstract class JobCreateRequest {

    @NotNull(message = "작업 종류는 필수입니다.")
    private JobType type;
}
