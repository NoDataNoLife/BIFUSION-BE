package com.nodatanolife.bifusion_be.domain.job.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationParameters;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AugmentationJobCreateRequest extends JobCreateRequest {

    @JsonProperty("num_samples_per_class")
    private Integer numSamplesPerClass;

    private Double strength;

    @JsonProperty("finetune_steps")
    private Integer finetuneSteps;

    private Integer steps;

    // 생략하면 BE가 랜덤 시드를 생성해 기록한다.
    private Long seed;

    @NotNull(message = "정상 이미지를 선택해 주세요.")
    private List<Long> normalFileIds;

    @NotNull(message = "비정상 이미지를 선택해 주세요.")
    private List<Long> anomalyFileIds;

    public AugmentationParameters toParameters() {
        return new AugmentationParameters(numSamplesPerClass, strength, finetuneSteps, steps, seed);
    }
}
