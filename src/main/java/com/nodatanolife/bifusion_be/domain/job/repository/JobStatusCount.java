package com.nodatanolife.bifusion_be.domain.job.repository;

import com.nodatanolife.bifusion_be.domain.job.entity.JobStatus;

/**
 * 프로젝트 Job 상태별 개수 조회 결과.
 */
public record JobStatusCount(JobStatus status, long count) {
}
