package com.nodatanolife.bifusion_be.domain.job.dto.response;

/**
 * 작업 종류별 결과. SUCCESS 상태일 때만 JobResponse.result에 담긴다.
 */
public sealed interface JobResult permits AugmentationJobResult, TrainingJobResult, InferenceJobResult {
}
