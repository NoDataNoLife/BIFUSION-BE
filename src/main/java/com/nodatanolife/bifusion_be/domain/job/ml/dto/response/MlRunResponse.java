package com.nodatanolife.bifusion_be.domain.job.ml.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * ML 실행(POST /run) 응답. job_id만 사용한다.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MlRunResponse(
        String jobId
) {
}
