package com.nodatanolife.bifusion_be.domain.job.service;

import com.nodatanolife.bifusion_be.domain.job.dto.response.JobResponse;
import com.nodatanolife.bifusion_be.domain.job.ml.MlClient;
import com.nodatanolife.bifusion_be.domain.job.ml.MlProperties;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

/**
 * AI Job 취소 흐름 조율. 트랜잭션을 갖지 않는다.
 * 1) 사전 확인(읽기 트랜잭션) → 2) ML 취소 요청(트랜잭션 밖) → 3) 반영(Job 단위 트랜잭션, Job을 다시 읽음)
 *
 * ml.cancel.enabled=false면 검증까지만 하고 ML 호출·상태 변경 없이 JOB_CANCEL_UNAVAILABLE.
 * (ML 취소 엔드포인트 배포 전에 BE만 CANCELLED가 되면 GPU 작업은 계속 돌고, 재시도 시 중복 실행되기 때문)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobCancelFacade {

    private final JobCommandService jobCommandService;
    private final JobQueryService jobQueryService;
    private final MlClient mlClient;
    private final MlProperties mlProperties;

    public JobResponse cancel(Long userId, Long jobId) {
        JobCancelPlan plan = jobCommandService.prepareCancel(userId, jobId);
        if (plan.alreadyCancelled()) {
            return jobQueryService.getJob(userId, jobId);
        }
        if (!mlProperties.cancel().enabled()) {
            throw new BusinessException(ErrorCode.JOB_CANCEL_UNAVAILABLE);
        }

        try {
            cancelOnMl(plan);
        } catch (BusinessException e) {
            switch (e.getErrorCode()) {
                // ML에 작업이 없으면 더 진행될 것도 없으므로 BE도 취소로 반영한다.
                case ML_JOB_NOT_FOUND -> log.info("[Cancel] ML에 작업 없음(404) - BE만 취소 반영, jobId: {}, mlJobId: {}",
                        jobId, plan.mlJobId());
                // ML에서 이미 끝났다. 상태는 바꾸지 않고, 폴러가 다음 주기에 실제 결과로 반영한다.
                case ML_JOB_ALREADY_FINISHED -> throw new BusinessException(ErrorCode.JOB_FINISHED_BEFORE_CANCEL);
                // 연결 실패(503)·응답 오류(502): 상태를 바꾸지 않고 그대로 응답한다.
                default -> throw e;
            }
        }

        applyCancel(jobId);
        return jobQueryService.getJob(userId, jobId);
    }

    /**
     * 폴러와 동시에 같은 Job을 바꾸면 낙관적 락 충돌이 난다. Job을 다시 읽어 한 번만 재판단한다.
     */
    private void applyCancel(Long jobId) {
        try {
            jobCommandService.applyCancel(jobId);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.info("[Cancel] 동시 변경 감지 - 다시 판단, jobId: {}", jobId);
            jobCommandService.applyCancel(jobId);
        }
    }

    private void cancelOnMl(JobCancelPlan plan) {
        switch (plan.type()) {
            case AUGMENTATION -> mlClient.cancelAugmentation(plan.mlJobId());
            case TRAINING -> mlClient.cancelTraining(plan.mlJobId());
            case INFERENCE -> mlClient.cancelInference(plan.mlJobId());
        }
    }
}
