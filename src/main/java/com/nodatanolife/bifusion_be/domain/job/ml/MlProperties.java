package com.nodatanolife.bifusion_be.domain.job.ml;

import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationParameters;
import com.nodatanolife.bifusion_be.domain.job.entity.TrainingParameters;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.List;

/**
 * ML 서버 연동 설정 (application.yml의 ml.*).
 *
 * @param baseUrl         ML 서버 주소. 비어 있으면 모든 호출을 연결 실패로 처리한다.
 * @param apiKey          X-API-Key 헤더 값. 비어 있으면 헤더를 붙이지 않는다.
 * @param pendingTimeout  ML 접수 후 PENDING으로 머무를 수 있는 최대 시간 (큐별 동시 실행 1개라 길게 잡는다)
 * @param maxInputSize    Base64 변환 전 입력 이미지 총 용량 상한
 * @param defaults        요청에서 생략된 파라미터를 채울 기본값
 * @param limits          Job 생성 요청의 파라미터 범위·이미지 장수 제한
 */
@ConfigurationProperties(prefix = "ml")
public record MlProperties(
        String baseUrl,
        String apiKey,
        Duration connectTimeout,
        Duration readTimeout,
        Duration pendingTimeout,
        DataSize maxInputSize,
        Defaults defaults,
        Limits limits
) {

    public record Defaults(
            AugmentationParameters augmentation,
            TrainingParameters training
    ) {
    }

    public record Limits(
            AugmentationLimits augmentation,
            TrainingLimits training,
            InferenceLimits inference
    ) {
    }

    public record AugmentationLimits(
            int minImagesPerClass,
            int maxImagesPerClass,
            int minNumSamplesPerClass,
            int maxNumSamplesPerClass,
            double minStrength,
            double maxStrength,
            int maxFinetuneSteps,
            int maxSteps
    ) {
    }

    public record TrainingLimits(
            List<Integer> kShotOptions,
            int maxEpochs,
            int maxQQuery,
            int maxEpisodesPerEpoch
    ) {
    }

    public record InferenceLimits(
            int minImages,
            int maxImages
    ) {
    }
}
