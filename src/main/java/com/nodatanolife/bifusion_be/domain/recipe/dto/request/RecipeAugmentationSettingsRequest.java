package com.nodatanolife.bifusion_be.domain.recipe.dto.request;

import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationParameters;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 레시피 증강 설정값. 이름과 범위는 증강 실행 요청과 같고, seed만 비워 둘 수 있다.
 * 범위는 JobRequestValidator가 yml(ml.limits) 기준으로 검증한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "레시피 증강 설정값 (증강 실행 파라미터와 같은 형식)")
public class RecipeAugmentationSettingsRequest {

    @Schema(description = "종류별 추가 생성 장수 (1~100)", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "클래스별 생성 장수를 입력해 주세요.")
    private Integer numSamplesPerClass;

    @Schema(description = "원본 변화 강도 (0~1)", requiredMode = Schema.RequiredMode.REQUIRED, example = "0.3")
    @NotNull(message = "증강 강도를 입력해 주세요.")
    private Double strength;

    @Schema(description = "업로드 이미지 맞춤 추가 학습 횟수 (1~1000)", requiredMode = Schema.RequiredMode.REQUIRED, example = "400")
    @NotNull(message = "파인튜닝 스텝을 입력해 주세요.")
    private Integer finetuneSteps;

    @Schema(description = "이미지 생성 계산 단계 수 (1~200)", requiredMode = Schema.RequiredMode.REQUIRED, example = "50")
    @NotNull(message = "샘플링 스텝을 입력해 주세요.")
    private Integer steps;

    // 비워 두면 이 레시피로 실행할 때 랜덤 시드를 쓴다
    @Schema(description = "시드 (0~4294967295). 비워 두면 실행할 때 랜덤 시드를 씁니다.", example = "42")
    private Long seed;

    public AugmentationParameters toParameters() {
        return new AugmentationParameters(numSamplesPerClass, strength, finetuneSteps, steps, seed);
    }
}
