package com.nodatanolife.bifusion_be.domain.job.service;

import com.nodatanolife.bifusion_be.domain.job.dto.response.JobResponse;

public interface JobQueryService {

    /** DB 값만 반환한다. 상태 갱신은 폴링 스케줄러가 한다. */
    JobResponse getJob(Long userId, Long jobId);

    /** 같은 멱등 키로 동시에 들어온 요청이 unique 제약에 걸렸을 때, 먼저 만들어진 Job을 조회한다. */
    JobResponse getJobByIdempotencyKey(Long userId, String idempotencyKey);
}
