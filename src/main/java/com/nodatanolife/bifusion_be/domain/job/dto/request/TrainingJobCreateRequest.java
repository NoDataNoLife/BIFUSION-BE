package com.nodatanolife.bifusion_be.domain.job.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nodatanolife.bifusion_be.domain.job.entity.TrainingParameters;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainingJobCreateRequest extends JobCreateRequest {

    // BE 증강 jobId (증강 결과의 datasetId)
    @NotNull(message = "학습에 사용할 증강 데이터를 선택해 주세요.")
    @JsonProperty("dataset_id")
    private Long datasetId;

    @JsonProperty("k_shot")
    private Integer kShot;

    private Integer epochs;

    @JsonProperty("q_query")
    private Integer qQuery;

    @JsonProperty("episodes_per_epoch")
    private Integer episodesPerEpoch;

    private Long seed;

    public TrainingParameters toParameters() {
        return new TrainingParameters(kShot, epochs, qQuery, episodesPerEpoch, seed);
    }
}
