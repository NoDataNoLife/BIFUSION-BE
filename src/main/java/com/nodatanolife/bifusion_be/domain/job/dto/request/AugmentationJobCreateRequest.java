package com.nodatanolife.bifusion_be.domain.job.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationParameters;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "증강 실행 요청 (type=AUGMENTATION). 생략한 설정값은 서버 기본값으로 채워집니다.")
public class AugmentationJobCreateRequest extends JobCreateRequest {

    @Schema(description = "종류별 추가 생성 장수 (기본 10, 1~100)", example = "10")
    @JsonProperty("num_samples_per_class")
    private Integer numSamplesPerClass;

    @Schema(description = "원본 변화 강도 (기본 0.3, 0~1)", example = "0.3")
    private Double strength;

    @Schema(description = "업로드 이미지 맞춤 추가 학습 횟수 (기본 400, 최대 1000)", example = "400")
    @JsonProperty("finetune_steps")
    private Integer finetuneSteps;

    @Schema(description = "이미지 생성 계산 단계 수 (기본 50, 최대 200)", example = "50")
    private Integer steps;

    // 생략하면 BE가 랜덤 시드를 생성해 기록한다.
    @Schema(description = "시드. 생략하면 서버가 랜덤 값을 만들어 기록합니다.", example = "42")
    private Long seed;

    @Schema(description = "정상 이미지 fileId 목록 (/api/v1/files/temp 업로드 결과, 5~10장)",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "[101, 102, 103, 104, 105]")
    @NotNull(message = "정상 이미지를 선택해 주세요.")
    private List<Long> normalFileIds;

    @Schema(description = "비정상 이미지 fileId 목록 (/api/v1/files/temp 업로드 결과, 5~10장)",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "[201, 202, 203, 204, 205]")
    @NotNull(message = "비정상 이미지를 선택해 주세요.")
    private List<Long> anomalyFileIds;

    public AugmentationParameters toParameters() {
        return new AugmentationParameters(numSamplesPerClass, strength, finetuneSteps, steps, seed);
    }
}
