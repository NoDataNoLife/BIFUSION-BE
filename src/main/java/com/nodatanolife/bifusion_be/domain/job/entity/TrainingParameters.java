package com.nodatanolife.bifusion_be.domain.job.entity;

/**
 * 학습 실행 파라미터 스냅샷 (training_config.parameters, jsonb).
 * ML 서버 기본값에 의존하지 않도록 BE가 모든 값을 채워 ML에 보내고, 보낸 값을 그대로 저장한다.
 */
public record TrainingParameters(
        Integer kShot,
        Integer epochs,
        Integer qQuery,
        Integer episodesPerEpoch,
        Long seed
) {

    public TrainingParameters withDefaults(TrainingParameters defaults) {
        return new TrainingParameters(
                kShot != null ? kShot : defaults.kShot(),
                epochs != null ? epochs : defaults.epochs(),
                qQuery != null ? qQuery : defaults.qQuery(),
                episodesPerEpoch != null ? episodesPerEpoch : defaults.episodesPerEpoch(),
                seed != null ? seed : defaults.seed()
        );
    }
}
