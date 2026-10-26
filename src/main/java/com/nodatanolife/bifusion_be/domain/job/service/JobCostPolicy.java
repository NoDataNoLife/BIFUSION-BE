package com.nodatanolife.bifusion_be.domain.job.service;

import com.nodatanolife.bifusion_be.domain.job.config.JobCostProperties;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationParameters;
import com.nodatanolife.bifusion_be.domain.job.entity.TrainingParameters;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * AI 작업 실행 비용(앤트) 계산. 기본값을 채운 최종 파라미터로 계산하므로 같은 설정이면 항상 같은 금액이다.
 * Job 생성·재시도(JobCommandServiceImpl)와 예상 비용 조회(JobCostEstimateService)가 같은 값을 쓴다.
 */
@Component
@RequiredArgsConstructor
public class JobCostPolicy {

    // 증강은 정상·비정상 두 종류를 각각 numSamplesPerClass장씩 만든다
    private static final int AUGMENTATION_CLASS_COUNT = 2;

    private final JobCostProperties properties;

    public int augmentation(AugmentationParameters params) {
        JobCostProperties.Augmentation cost = properties.augmentation();
        long generationSteps = (long) AUGMENTATION_CLASS_COUNT * params.numSamplesPerClass() * params.steps();
        return Math.toIntExact(cost.base()
                + ceilDiv(params.finetuneSteps(), cost.finetuneStepsPerPoint())
                + ceilDiv(generationSteps, cost.generationStepsPerPoint()));
    }

    public int training(TrainingParameters params) {
        JobCostProperties.Training cost = properties.training();
        long episodes = (long) params.epochs() * params.episodesPerEpoch();
        return Math.toIntExact(cost.base() + ceilDiv(episodes, cost.episodesPerPoint()));
    }

    public int inference(int imageCount) {
        JobCostProperties.Inference cost = properties.inference();
        return Math.toIntExact(cost.base() + (long) imageCount * cost.pointsPerImage());
    }

    private static long ceilDiv(long value, int divisor) {
        return Math.ceilDiv(value, divisor);
    }
}
