package com.nodatanolife.bifusion_be.domain.job.service;

import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationParameters;
import com.nodatanolife.bifusion_be.domain.job.entity.TrainingParameters;
import com.nodatanolife.bifusion_be.domain.job.ml.MlProperties;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Job 생성 요청의 파라미터 범위·이미지 장수 검증. 범위는 yml(ml.limits)에서 읽는다.
 * 기본값을 채운 최종 파라미터를 검증해 실제로 ML에 보낼 값이 범위 안에 있도록 한다.
 */
@Component
@RequiredArgsConstructor
public class JobRequestValidator {

    private static final long MAX_SEED = 4_294_967_295L; // ML(numpy) 시드 상한 2^32 - 1

    private final MlProperties mlProperties;

    public void validateAugmentation(AugmentationParameters params, List<Long> normalFileIds, List<Long> anomalyFileIds) {
        MlProperties.AugmentationLimits limits = mlProperties.limits().augmentation();
        requireRange(params.numSamplesPerClass(), limits.minNumSamplesPerClass(), limits.maxNumSamplesPerClass());
        if (params.strength() < limits.minStrength() || params.strength() > limits.maxStrength()) {
            throw new BusinessException(ErrorCode.INVALID_JOB_PARAMETER);
        }
        requireRange(params.finetuneSteps(), 1, limits.maxFinetuneSteps());
        requireRange(params.steps(), 1, limits.maxSteps());
        requireSeed(params.seed());

        requireImageCount(normalFileIds, limits.minImagesPerClass(), limits.maxImagesPerClass());
        requireImageCount(anomalyFileIds, limits.minImagesPerClass(), limits.maxImagesPerClass());
        requireDistinct(Stream.concat(normalFileIds.stream(), anomalyFileIds.stream()).toList());
    }

    public void validateTraining(TrainingParameters params) {
        MlProperties.TrainingLimits limits = mlProperties.limits().training();
        if (!limits.kShotOptions().contains(params.kShot())) {
            throw new BusinessException(ErrorCode.INVALID_JOB_PARAMETER);
        }
        requireRange(params.epochs(), 1, limits.maxEpochs());
        requireRange(params.qQuery(), 1, limits.maxQQuery());
        requireRange(params.episodesPerEpoch(), 1, limits.maxEpisodesPerEpoch());
        requireSeed(params.seed());
    }

    public void validateInference(List<Long> fileIds) {
        MlProperties.InferenceLimits limits = mlProperties.limits().inference();
        requireImageCount(fileIds, limits.minImages(), limits.maxImages());
        requireDistinct(fileIds);
    }

    private static void requireRange(int value, int min, int max) {
        if (value < min || value > max) {
            throw new BusinessException(ErrorCode.INVALID_JOB_PARAMETER);
        }
    }

    private static void requireSeed(long seed) {
        if (seed < 0 || seed > MAX_SEED) {
            throw new BusinessException(ErrorCode.INVALID_JOB_PARAMETER);
        }
    }

    private static void requireImageCount(List<Long> fileIds, int min, int max) {
        if (fileIds.size() < min || fileIds.size() > max) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_COUNT);
        }
    }

    private static void requireDistinct(List<Long> fileIds) {
        Set<Long> seen = new HashSet<>();
        for (Long fileId : fileIds) {
            if (fileId == null || !seen.add(fileId)) {
                throw new BusinessException(ErrorCode.DUPLICATE_INPUT_FILE);
            }
        }
    }
}
