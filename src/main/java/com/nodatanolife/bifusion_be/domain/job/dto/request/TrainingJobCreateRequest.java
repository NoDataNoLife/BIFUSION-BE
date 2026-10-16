package com.nodatanolife.bifusion_be.domain.job.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nodatanolife.bifusion_be.domain.job.entity.TrainingParameters;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "학습 실행 요청 (type=TRAINING). 생략한 설정값은 서버 기본값으로 채워집니다.")
public class TrainingJobCreateRequest extends JobCreateRequest {

    // BE 증강 jobId (증강 결과의 datasetId)
    @Schema(description = "학습에 쓸 증강 데이터 (증강 데이터셋 목록의 datasetId)",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "15")
    @NotNull(message = "학습에 사용할 증강 데이터를 선택해 주세요.")
    @JsonProperty("dataset_id")
    private Long datasetId;

    // getKShot()의 Jackson 기본 이름(kshot)이 필드와 어긋나지 않도록 getter에도 같은 이름을 지정한다.
    @Schema(description = "K-shot (1, 3, 5, 10 중 하나, 기본 5)", example = "5")
    @JsonProperty("k_shot")
    @Getter(onMethod_ = @JsonProperty("k_shot"))
    private Integer kShot;

    @Schema(description = "학습 반복 횟수 (기본 30, 최대 200)", example = "30")
    private Integer epochs;

    @Schema(description = "에피소드당 연습 이미지 수 (기본 5, 최대 20)", example = "5")
    @JsonProperty("q_query")
    @Getter(onMethod_ = @JsonProperty("q_query"))
    private Integer qQuery;

    @Schema(description = "학습 반복당 에피소드 수 (기본 10, 최대 100)", example = "10")
    @JsonProperty("episodes_per_epoch")
    private Integer episodesPerEpoch;

    @Schema(description = "시드 (기본 42)", example = "42")
    private Long seed;

    public TrainingParameters toParameters() {
        return new TrainingParameters(kShot, epochs, qQuery, episodesPerEpoch, seed);
    }
}
