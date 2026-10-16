package com.nodatanolife.bifusion_be.domain.job.entity;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 증강 실행 파라미터 스냅샷 (augmentation_config.parameters, jsonb).
 * ML 서버 기본값에 의존하지 않도록 BE가 모든 값을 채워 ML에 보내고, 보낸 값을 그대로 저장한다.
 */
public record AugmentationParameters(
        Integer numSamplesPerClass,
        Double strength,
        Integer finetuneSteps,
        Integer steps,
        Long seed
) {

    /**
     * 비어 있는 값을 기본값(yml)으로 채운다. seed는 기본값이 없으면 재현 가능하도록 랜덤 값을 만들어 기록한다.
     */
    public AugmentationParameters withDefaults(AugmentationParameters defaults) {
        return new AugmentationParameters(
                numSamplesPerClass != null ? numSamplesPerClass : defaults.numSamplesPerClass(),
                strength != null ? strength : defaults.strength(),
                finetuneSteps != null ? finetuneSteps : defaults.finetuneSteps(),
                steps != null ? steps : defaults.steps(),
                seed != null ? seed : randomSeed(defaults.seed())
        );
    }

    // ML(numpy) 시드 범위와 FE 정수 범위를 모두 만족하도록 int 범위에서 생성한다.
    private static Long randomSeed(Long defaultSeed) {
        return defaultSeed != null ? defaultSeed : (long) ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
    }
}
