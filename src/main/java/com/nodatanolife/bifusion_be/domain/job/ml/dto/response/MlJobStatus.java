package com.nodatanolife.bifusion_be.domain.job.ml.dto.response;

import com.nodatanolife.bifusion_be.domain.job.entity.JobStatus;

/**
 * ML 조회 응답의 status 원문.
 */
public enum MlJobStatus {
    PENDING,
    STARTED,
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELLED;

    public JobStatus toJobStatus() {
        return switch (this) {
            case PENDING -> JobStatus.PENDING;
            case STARTED, RUNNING -> JobStatus.RUNNING;
            case SUCCESS -> JobStatus.SUCCESS;
            case FAILED -> JobStatus.FAILED;
            case CANCELLED -> JobStatus.CANCELLED;
        };
    }
}
