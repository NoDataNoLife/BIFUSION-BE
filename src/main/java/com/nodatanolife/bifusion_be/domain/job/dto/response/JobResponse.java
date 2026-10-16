package com.nodatanolife.bifusion_be.domain.job.dto.response;

import com.nodatanolife.bifusion_be.domain.job.entity.Job;
import com.nodatanolife.bifusion_be.domain.job.entity.JobStatus;
import com.nodatanolife.bifusion_be.domain.job.entity.JobType;

import java.time.LocalDateTime;

/**
 * AI Job 생성·조회 응답. FE parseJob 형식을 따른다.
 * error는 사용자용 문구(errorMessage)만 담고, ML 원문(mlErrorDetail)은 절대 포함하지 않는다.
 * retryable이 true일 때만 FE가 "다시 시도"를 보여준다. retryOfJobId는 이 Job이 재시도한 원본 jobId(없으면 null).
 */
public record JobResponse(
        Long jobId,
        JobType type,
        Long projectId,
        JobStatus status,
        Integer progress,
        String currentStep,
        String seedNumber,
        JobResult result,
        String error,
        boolean retryable,
        Long retryOfJobId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static JobResponse from(Job job, JobResult result) {
        return new JobResponse(
                job.getJobId(),
                job.getType(),
                job.getProject().getProjectId(),
                job.getStatus(),
                job.getProgress(),
                job.getCurrentStep(),
                job.getSeedNumber(),
                job.getStatus() == JobStatus.SUCCESS ? result : null,
                job.getErrorMessage(),
                job.isRetryable(),
                job.getRetryOfJobId(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}
