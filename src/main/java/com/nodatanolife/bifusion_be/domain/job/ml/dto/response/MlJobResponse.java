package com.nodatanolife.bifusion_be.domain.job.ml.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nodatanolife.bifusion_be.domain.job.entity.JobFailureReason;
import com.nodatanolife.bifusion_be.domain.job.entity.JobStatus;

/**
 * ML 조회(GET /{job_id}) 응답.
 * 완료 판정은 status로만 하며 progress는 사용하지 않는다.
 * error_code / current_step은 ML 제안 단계 필드라 없을 수 있다.
 *
 * @param error ML 원문. 내부 경로·traceback이 섞일 수 있으므로 Job.mlErrorDetail에만 저장하고 FE로 내보내지 않는다.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MlJobResponse<T>(
        String jobId,
        MlJobStatus status,
        T result,
        String error,
        String errorCode,
        String currentStep
) {

    public JobStatus jobStatus() {
        return status.toJobStatus();
    }

    /**
     * 사용자용 실패 사유 판정만 한다.
     * TODO: (이슈 C) 스케줄러에서 UNKNOWN이면 운영 확인용 error 로그를 남길 것 (mlJobId, errorCode, error 원문)
     */
    public JobFailureReason failureReason() {
        return JobFailureReason.resolve(errorCode, error);
    }
}
