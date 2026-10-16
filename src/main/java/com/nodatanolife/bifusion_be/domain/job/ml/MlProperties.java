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
 * @param polling         ML 작업 상태 폴링 스케줄러 설정
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
        Limits limits,
        Polling polling
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

    /**
     * @param interval       폴링 주기 (이전 실행이 끝난 뒤부터 잼)
     * @param batchSize      한 주기에 처리할 최대 Job 수
     * @param launchTimeout  mlJobId 없는 PENDING Job을 실행 실패로 보는 기준 (생성 시각부터)
     * @param runningTimeout RUNNING Job의 최대 허용 시간 (ML 접수 시각부터)
     */
    public record Polling(
            Duration interval,
            int batchSize,
            Duration launchTimeout,
            Duration runningTimeout
    ) {
    }

    public record InferenceLimits(
            int minImages,
            int maxImages
    ) {
    }
}
