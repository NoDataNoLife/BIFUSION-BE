package com.nodatanolife.bifusion_be.domain.job.service;

import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationParameters;
import com.nodatanolife.bifusion_be.domain.job.entity.JobType;
import com.nodatanolife.bifusion_be.domain.job.entity.TrainingParameters;

import java.util.List;

/**
 * 트랜잭션 1(Job 생성) 결과로, 트랜잭션 밖에서 ML 실행 요청을 만들 때 필요한 값을 담는다.
 * existing이면 같은 멱등 키로 이미 만들어진 Job이므로 ML을 호출하지 않는다.
 * retry면 재시도 Job이며, 입력 파일 확인을 소유자 대신 "이 Job의 입력 파일인지"로 한다.
 */
public record JobLaunchPlan(
        Long jobId,
        boolean existing,
        boolean retry,
        JobType type,
        AugmentationParameters augmentationParameters,
        List<Long> normalFileIds,
        List<Long> anomalyFileIds,
        TrainingParameters trainingParameters,
        String mlDatasetId,
        String mlModelId,
        List<Long> inferenceFileIds
) {

    public static JobLaunchPlan existing(Long jobId, JobType type) {
        return new JobLaunchPlan(jobId, true, false, type, null, null, null, null, null, null, null);
    }

    public static JobLaunchPlan augmentation(Long jobId, AugmentationParameters parameters,
                                             List<Long> normalFileIds, List<Long> anomalyFileIds) {
        return new JobLaunchPlan(jobId, false, false, JobType.AUGMENTATION, parameters, normalFileIds, anomalyFileIds,
                null, null, null, null);
    }

    public static JobLaunchPlan training(Long jobId, TrainingParameters parameters, String mlDatasetId) {
        return new JobLaunchPlan(jobId, false, false, JobType.TRAINING, null, null, null, parameters, mlDatasetId,
                null, null);
    }

    public static JobLaunchPlan inference(Long jobId, String mlModelId, List<Long> fileIds) {
        return new JobLaunchPlan(jobId, false, false, JobType.INFERENCE, null, null, null, null, null,
                mlModelId, fileIds);
    }

    public JobLaunchPlan asRetry() {
        return new JobLaunchPlan(jobId, existing, true, type, augmentationParameters, normalFileIds, anomalyFileIds,
                trainingParameters, mlDatasetId, mlModelId, inferenceFileIds);
    }
}
