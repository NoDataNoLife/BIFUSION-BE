package com.nodatanolife.bifusion_be.domain.job.service;

import com.nodatanolife.bifusion_be.domain.job.entity.JobType;

/**
 * 취소 사전 확인 결과. alreadyCancelled면 이미 취소된 Job이라 ML을 호출하지 않고 그대로 응답한다.
 */
public record JobCancelPlan(
        Long jobId,
        JobType type,
        String mlJobId,
        boolean alreadyCancelled
) {
}
