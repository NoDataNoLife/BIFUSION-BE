package com.nodatanolife.bifusion_be.domain.job.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 작업 종류별 결과. SUCCESS 상태일 때만 JobResponse.result에 담긴다.
 */
@Schema(description = "작업 종류별 결과", oneOf = {AugmentationJobResult.class, TrainingJobResult.class, InferenceJobResult.class})
public sealed interface JobResult permits AugmentationJobResult, TrainingJobResult, InferenceJobResult {
}
