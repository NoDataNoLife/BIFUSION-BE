package com.nodatanolife.bifusion_be.domain.job.service;

import com.nodatanolife.bifusion_be.domain.job.entity.Job;
import com.nodatanolife.bifusion_be.domain.job.entity.JobFailureReason;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.response.MlAugmentationResult;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.response.MlInferenceResult;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.response.MlTrainingResult;

import java.util.List;

/**
 * 폴링 결과를 Job에 반영한다. 메서드 하나가 Job 하나의 트랜잭션이다.
 * 모든 반영 메서드는 Job을 다시 읽고, 이미 종료 상태면 아무것도 하지 않는다.
 */
public interface JobPollingService {

    /** 폴링 대상(PENDING·RUNNING) Job. 반환된 엔티티는 판정용 스냅샷으로만 쓴다. */
    List<Job> findPollTargets();

    /**
     * @return 실제로 실패 처리했으면 true. 이미 종료 상태라 아무것도 하지 않았으면 false
     */
    boolean fail(Long jobId, JobFailureReason reason, String mlErrorDetail);

    void applyProgress(Long jobId, boolean running, String currentStep);

    void cancel(Long jobId);

    void applyAugmentationSuccess(Long jobId, MlAugmentationResult result, List<ResultFileMeta> resultFiles);

    void applyTrainingSuccess(Long jobId, MlTrainingResult result, List<ResultFileMeta> resultFiles);

    void applyInferenceSuccess(Long jobId, MlInferenceResult result);
}
