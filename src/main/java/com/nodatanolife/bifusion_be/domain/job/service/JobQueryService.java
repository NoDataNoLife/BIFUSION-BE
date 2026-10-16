package com.nodatanolife.bifusion_be.domain.job.service;

import com.nodatanolife.bifusion_be.domain.job.dto.response.JobResponse;
import com.nodatanolife.bifusion_be.domain.job.dto.response.ProjectJobListResponse;
import com.nodatanolife.bifusion_be.domain.job.entity.JobStatus;
import com.nodatanolife.bifusion_be.domain.job.entity.JobType;

public interface JobQueryService {

    /** DB 값만 반환한다. 상태 갱신은 폴링 스케줄러가 한다. */
    JobResponse getJob(Long userId, Long jobId);

    /**
     * 프로젝트 Job 목록 (최신순, 커서 기반). DB 값만 반환한다.
     * statusCounts는 첫 페이지(cursor 없음)에서만 계산한다.
     */
    ProjectJobListResponse getProjectJobs(Long userId, Long projectId, JobType type, JobStatus status,
                                          String keyword, String cursor, int size);

    /** 같은 멱등 키로 동시에 들어온 요청이 unique 제약에 걸렸을 때, 먼저 만들어진 Job을 조회한다. */
    JobResponse getJobByIdempotencyKey(Long userId, String idempotencyKey);
}
