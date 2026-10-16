package com.nodatanolife.bifusion_be.domain.job.service;

import com.nodatanolife.bifusion_be.domain.file.service.S3ObjectInfo;
import com.nodatanolife.bifusion_be.domain.file.service.S3Service;
import com.nodatanolife.bifusion_be.domain.job.entity.Job;
import com.nodatanolife.bifusion_be.domain.job.entity.JobFailureReason;
import com.nodatanolife.bifusion_be.domain.job.entity.JobStatus;
import com.nodatanolife.bifusion_be.domain.job.ml.MlClient;
import com.nodatanolife.bifusion_be.domain.job.ml.MlProperties;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.response.MlAugmentationResult;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.response.MlInferenceResult;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.response.MlJobResponse;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.response.MlTrainingResult;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Job 1건 폴링. 트랜잭션을 갖지 않는다.
 * 사전 검사(타임아웃) → ML 조회(트랜잭션 밖) → SUCCESS면 결과 파일 S3 조회(트랜잭션 밖) → JobPollingService로 반영(Job 단위 트랜잭션).
 *
 * 로그에는 jobId·mlJobId·사유만 남긴다. ML 응답(MlJobResponse)은 error 원문을 포함하므로 통째로 로그에 남기지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MlJobPoller {

    private final MlClient mlClient;
    private final JobPollingService jobPollingService;
    private final S3Service s3Service;
    private final MlProperties mlProperties;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    /**
     * @param job 폴링 대상 조회 시점의 스냅샷 (판정용). 상태 변경은 반영 서비스가 Job을 다시 읽어 수행한다.
     */
    public void poll(Job job, PollingCycle cycle) {
        LocalDateTime now = LocalDateTime.now();
        MlProperties.Polling polling = mlProperties.polling();

        if (job.getMlJobId() == null) {
            // 실행 요청 전에 서버가 죽어 남은 Job. 그 전이면 아직 실행 요청 중일 수 있으므로 건너뛴다.
            if (job.isLaunchTimedOut(polling.launchTimeout(), now)) {
                log.warn("[Polling] ML 실행 요청 없이 남은 Job 실패 처리 - jobId: {}", job.getJobId());
                jobPollingService.fail(job.getJobId(), JobFailureReason.ML_SUBMIT_UNCERTAIN, null);
            }
            return;
        }
        // ML은 모르는 job_id에도 PENDING을 주므로, ML 조회 없이 시간으로 판정한다.
        if (job.isPendingTimedOut(mlProperties.pendingTimeout(), now)) {
            log.warn("[Polling] PENDING 타임아웃 - jobId: {}, mlJobId: {}", job.getJobId(), job.getMlJobId());
            jobPollingService.fail(job.getJobId(), JobFailureReason.PENDING_TIMEOUT, null);
            return;
        }
        if (job.isRunningTimedOut(polling.runningTimeout(), now)) {
            // TODO: 취소 API 구현 후 ML 작업 취소 호출 (지금은 ML 쪽 작업이 계속 돌 수 있음)
            log.warn("[Polling] RUNNING 타임아웃 - jobId: {}, mlJobId: {}", job.getJobId(), job.getMlJobId());
            jobPollingService.fail(job.getJobId(), JobFailureReason.RUNNING_TIMEOUT, null);
            return;
        }
        if (cycle.isMlUnavailable()) {
            cycle.skipMlCall();
            return;
        }

        try {
            switch (job.getType()) {
                case AUGMENTATION -> pollAugmentation(job, now);
                case TRAINING -> pollTraining(job, now);
                case INFERENCE -> pollInference(job, now);
            }
        } catch (BusinessException e) {
            switch (e.getErrorCode()) {
                case ML_JOB_NOT_FOUND -> {
                    log.warn("[Polling] ML에 작업 없음(404) - jobId: {}, mlJobId: {}", job.getJobId(), job.getMlJobId());
                    jobPollingService.fail(job.getJobId(), JobFailureReason.ML_JOB_NOT_FOUND, null);
                }
                // 실패 처리하지 않고 다음 주기에 재시도. 로그는 스케줄러가 주기마다 한 줄로 요약한다.
                case ML_CONNECTION_FAILED -> cycle.markMlUnavailable();
                case ML_RESPONSE_ERROR -> log.warn("[Polling] ML 응답 처리 실패 - 다음 주기에 재시도, jobId: {}, mlJobId: {}",
                        job.getJobId(), job.getMlJobId());
                default -> throw e;
            }
        }
    }

    private void pollAugmentation(Job job, LocalDateTime now) {
        MlJobResponse<MlAugmentationResult> response = mlClient.getAugmentation(job.getMlJobId());
        if (handleNonSuccess(job, response, now)) {
            return;
        }
        MlAugmentationResult result = response.result();
        jobPollingService.applyAugmentationSuccess(job.getJobId(), result, describeResultFiles(job,
                result.datasetStorageKey(bucketName), result.previewStorageKey(bucketName)));
    }

    private void pollTraining(Job job, LocalDateTime now) {
        MlJobResponse<MlTrainingResult> response = mlClient.getTraining(job.getMlJobId());
        if (handleNonSuccess(job, response, now)) {
            return;
        }
        MlTrainingResult result = response.result();
        jobPollingService.applyTrainingSuccess(job.getJobId(), result,
                describeResultFiles(job, result.modelStorageKey(bucketName)));
    }

    private void pollInference(Job job, LocalDateTime now) {
        MlJobResponse<MlInferenceResult> response = mlClient.getInference(job.getMlJobId());
        if (handleNonSuccess(job, response, now)) {
            return;
        }
        jobPollingService.applyInferenceSuccess(job.getJobId(), response.result());
    }

    /**
     * SUCCESS가 아닌 응답을 반영한다. 반영했으면 true, SUCCESS라 결과 저장이 필요하면 false.
     * 완료 판정은 status로만 하며 progress는 쓰지 않는다.
     */
    private boolean handleNonSuccess(Job job, MlJobResponse<?> response, LocalDateTime now) {
        Long jobId = job.getJobId();
        String currentStep = response.currentStep();
        boolean stepChanged = currentStep != null && !currentStep.equals(job.getCurrentStep());

        switch (response.jobStatus()) {
            case PENDING -> {
                if (job.isStaleSince(mlProperties.pendingTimeout(), now)) {
                    log.warn("[Polling] RUNNING이던 작업을 ML이 PENDING으로 응답 - 유실로 판단, jobId: {}, mlJobId: {}",
                            jobId, job.getMlJobId());
                    jobPollingService.fail(jobId, JobFailureReason.ML_JOB_NOT_FOUND, null);
                } else if (stepChanged) {
                    jobPollingService.applyProgress(jobId, false, currentStep);
                }
                return true;
            }
            case RUNNING -> {
                if (job.getStatus() == JobStatus.PENDING || stepChanged) {
                    jobPollingService.applyProgress(jobId, true, currentStep);
                }
                return true;
            }
            case FAILED -> {
                JobFailureReason reason = response.failureReason();
                if (reason == JobFailureReason.UNKNOWN) {
                    log.error("[Polling] 분류되지 않은 ML 작업 실패 - jobId: {}, mlJobId: {}, errorCode: {}",
                            jobId, job.getMlJobId(), response.errorCode());
                } else {
                    log.info("[Polling] ML 작업 실패 - jobId: {}, mlJobId: {}, 사유: {}", jobId, job.getMlJobId(), reason);
                }
                jobPollingService.fail(jobId, reason, response.error());
                return true;
            }
            case CANCELLED -> {
                jobPollingService.cancel(jobId);
                return true;
            }
            case SUCCESS -> {
                if (response.result() == null) {
                    log.error("[Polling] ML SUCCESS 응답에 result 없음 - jobId: {}, mlJobId: {}", jobId, job.getMlJobId());
                    jobPollingService.fail(jobId, JobFailureReason.UNKNOWN, "SUCCESS 응답에 result 없음");
                    return true;
                }
                return false;
            }
        }
        return true;
    }

    /**
     * 결과 파일 key마다 S3에서 크기·형식을 미리 조회한다 (트랜잭션 밖). 조회에 실패하면 값 없이 진행한다.
     * key가 없으면(ML 로컬 저장 또는 다른 버킷) File 행을 만들지 않는다.
     */
    private List<ResultFileMeta> describeResultFiles(Job job, String... s3Keys) {
        List<String> keys = Stream.of(s3Keys).filter(Objects::nonNull).toList();
        if (keys.size() < s3Keys.length) {
            log.warn("[Polling] 결과 파일 S3 key 없음(ML 로컬 저장 또는 다른 버킷) - jobId: {}, mlJobId: {}",
                    job.getJobId(), job.getMlJobId());
        }
        List<ResultFileMeta> metas = new ArrayList<>();
        for (String key : keys) {
            S3ObjectInfo info = s3Service.headObject(key).orElse(null);
            metas.add(new ResultFileMeta(key,
                    info != null ? info.size() : null,
                    info != null ? info.contentType() : null));
        }
        return metas;
    }
}
