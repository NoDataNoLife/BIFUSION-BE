package com.nodatanolife.bifusion_be.domain.job.service;

import com.nodatanolife.bifusion_be.domain.job.dto.request.JobCreateRequest;
import com.nodatanolife.bifusion_be.domain.job.entity.JobFailureReason;

public interface JobCommandService {

    /**
     * 트랜잭션 1: 권한·검증 후 PENDING Job과 Config를 만들고 입력 파일을 Job에 연결한다.
     * 같은 사용자가 같은 멱등 키로 요청했으면 새로 만들지 않고 기존 Job을 가리키는 계획을 반환한다.
     */
    JobLaunchPlan createPending(Long userId, Long projectId, JobCreateRequest request, String idempotencyKey);

    /** 트랜잭션 2: ML 접수 성공 */
    void markStarted(Long jobId, String mlJobId);

    /** 트랜잭션 2: ML 실행 전·접수 단계 실패. 입력 파일은 Job에 연결된 채로 보존한다(재시도 기능 대비). */
    void markFailed(Long jobId, JobFailureReason reason);
}
