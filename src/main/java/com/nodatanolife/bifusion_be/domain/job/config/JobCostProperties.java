package com.nodatanolife.bifusion_be.domain.job.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 작업 실행 비용 (job.cost). 1토큰 = 1앤트. ML이 실제 사용량을 주지 않아 실행 파라미터로 계산량을 추정한다.
 * 계산식은 JobCostPolicy에 있다.
 *
 * @param augmentation 증강: base + ceil(finetuneSteps / finetuneStepsPerPoint) + ceil(클래스 2개 × 샘플 수 × steps / generationStepsPerPoint)
 * @param training     학습: base + ceil(epochs × episodesPerEpoch / episodesPerPoint)
 * @param inference    추론: base + 이미지 수 × pointsPerImage
 */
@ConfigurationProperties(prefix = "job.cost")
public record JobCostProperties(
        Augmentation augmentation,
        Training training,
        Inference inference
) {

    public record Augmentation(
            int base,
            int finetuneStepsPerPoint,
            int generationStepsPerPoint
    ) {
    }

    public record Training(
            int base,
            int episodesPerPoint
    ) {
    }

    public record Inference(
            int base,
            int pointsPerImage
    ) {
    }
}
