package com.nodatanolife.bifusion_be.domain.job.service;

import com.nodatanolife.bifusion_be.domain.file.service.S3ObjectInfo;
import com.nodatanolife.bifusion_be.domain.file.service.S3ObjectLookup;
import com.nodatanolife.bifusion_be.domain.file.service.S3Service;
import com.nodatanolife.bifusion_be.domain.job.entity.Job;
import com.nodatanolife.bifusion_be.domain.job.entity.JobFailureReason;
import com.nodatanolife.bifusion_be.domain.job.entity.JobStatus;
import com.nodatanolife.bifusion_be.domain.job.ml.MlClient;
import com.nodatanolife.bifusion_be.domain.job.ml.MlProperties;
import com.nodatanolife.bifusion_be.domain.job.ml.MlUnavailableException;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.response.MlArtifact;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.response.MlAugmentationResult;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.response.MlInferenceResult;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.response.MlJobResponse;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.response.MlTrainingResult;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private final MlConnectionState mlConnectionState;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    // PENDING 지속 경고를 마지막으로 남긴 시각 (jobId별). 메모리에만 두며 서버 재시작 시 초기화된다.
    private final Map<Long, LocalDateTime> pendingWarnedAt = new ConcurrentHashMap<>();

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
            if (jobPollingService.fail(job.getJobId(), JobFailureReason.PENDING_TIMEOUT, null)) {
                cancelOnMlQuietly(job, cycle);
            }
            return;
        }
        if (job.isRunningTimedOut(polling.runningTimeout(), now)) {
            log.warn("[Polling] RUNNING 타임아웃 - jobId: {}, mlJobId: {}", job.getJobId(), job.getMlJobId());
            if (jobPollingService.fail(job.getJobId(), JobFailureReason.RUNNING_TIMEOUT, null)) {
                cancelOnMlQuietly(job, cycle);
            }
            return;
        }
        // 연결 불가가 이어지는 동안에는 ML 조회 없이 시간으로 판정한다 (ML이 작업을 계속하는지 알 수 없다).
        if (isUnreachableTimedOut(job, now)) {
            log.warn("[Polling] ML 연결 불가 지속 - 실패 처리, jobId: {}, mlJobId: {}, 연결 불가 시작: {}",
                    job.getJobId(), job.getMlJobId(), mlConnectionState.getUnreachableSince());
            jobPollingService.fail(job.getJobId(), JobFailureReason.ML_UNREACHABLE, null);
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
                    markMlReachable(now);
                    log.warn("[Polling] ML에 작업 없음(404) - jobId: {}, mlJobId: {}", job.getJobId(), job.getMlJobId());
                    jobPollingService.fail(job.getJobId(), JobFailureReason.ML_JOB_NOT_FOUND, null);
                }
                // 실패 처리하지 않고 다음 주기에 재시도. 로그는 스케줄러가 주기마다 한 줄로 요약한다.
                // 연결 불가가 unreachableTimeout 넘게 이어지면 isUnreachableTimedOut으로 실패 처리된다.
                case ML_CONNECTION_FAILED -> {
                    markMlUnreachable(now);
                    cycle.markMlUnavailable(unavailableReason(e));
                }
                case ML_RESPONSE_ERROR -> {
                    markMlReachable(now);
                    log.warn("[Polling] ML 응답 처리 실패 - 다음 주기에 재시도, jobId: {}, mlJobId: {}",
                            job.getJobId(), job.getMlJobId());
                }
                default -> throw e;
            }
        }
    }

    /**
     * 타임아웃으로 실패 처리한 Job의 ML 작업을 멈추도록 취소를 한 번만 시도한다 (트랜잭션 밖).
     * 결과와 관계없이 Job 상태는 바꾸지 않는다(이미 FAILED). 실패는 로그만 남기고 무시한다.
     * ml.cancel.enabled=false이거나 이번 주기에 ML 연결 실패가 확인됐으면 시도하지 않는다.
     */
    private void cancelOnMlQuietly(Job job, PollingCycle cycle) {
        if (!mlProperties.cancel().enabled()) {
            return;
        }
        if (cycle.isMlUnavailable()) {
            log.info("[Polling] ML 연결 불가로 타임아웃 Job의 ML 취소 생략 - jobId: {}, mlJobId: {}",
                    job.getJobId(), job.getMlJobId());
            return;
        }
        try {
            switch (job.getType()) {
                case AUGMENTATION -> mlClient.cancelAugmentation(job.getMlJobId());
                case TRAINING -> mlClient.cancelTraining(job.getMlJobId());
                case INFERENCE -> mlClient.cancelInference(job.getMlJobId());
            }
            log.info("[Polling] 타임아웃 Job의 ML 취소 요청 완료 - jobId: {}, mlJobId: {}", job.getJobId(), job.getMlJobId());
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.ML_CONNECTION_FAILED) {
                cycle.markMlUnavailable(unavailableReason(e));
            }
            log.warn("[Polling] 타임아웃 Job의 ML 취소 실패(무시) - jobId: {}, mlJobId: {}, code: {}",
                    job.getJobId(), job.getMlJobId(), e.getErrorCode());
        }
    }

    private void pollAugmentation(Job job, LocalDateTime now) {
        MlJobResponse<MlAugmentationResult> response = mlClient.getAugmentation(job.getMlJobId());
        if (handleNonSuccess(job, response, now)) {
            return;
        }
        MlAugmentationResult result = response.result();
        jobPollingService.applyAugmentationSuccess(job.getJobId(), result,
                describeResultFile(job, "dataset", result.download()),
                describeResultFile(job, "preview", result.preview()));
    }

    private void pollTraining(Job job, LocalDateTime now) {
        MlJobResponse<MlTrainingResult> response = mlClient.getTraining(job.getMlJobId());
        if (handleNonSuccess(job, response, now)) {
            return;
        }
        MlTrainingResult result = response.result();
        jobPollingService.applyTrainingSuccess(job.getJobId(), result,
                describeResultFile(job, "model", result.download()));
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

        markMlReachable(now);
        if (response.jobStatus() != JobStatus.PENDING) {
            pendingWarnedAt.remove(jobId);
        }
        switch (response.jobStatus()) {
            case PENDING -> {
                if (job.isStaleSince(mlProperties.pendingTimeout(), now)) {
                    log.warn("[Polling] RUNNING이던 작업을 ML이 PENDING으로 응답 - 유실로 판단, jobId: {}, mlJobId: {}",
                            jobId, job.getMlJobId());
                    jobPollingService.fail(jobId, JobFailureReason.ML_JOB_NOT_FOUND, null);
                    return true;
                }
                warnIfPendingTooLong(job, now);
                if (stepChanged) {
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
     * ML이 PENDING을 pendingWarnAfter 넘게 돌려주면 경고 로그를 남긴다. 같은 Job은 pendingWarnAfter 간격으로만 다시 남긴다.
     * ML은 모르는 job_id에도 PENDING을 주므로(작업 유실, started 추적 미설정 포함) 이 로그 없이는 원인을 구분할 수 없다.
     * 상태는 바꾸지 않는다 (실패 처리는 pendingTimeout이 맡는다).
     */
    private void warnIfPendingTooLong(Job job, LocalDateTime now) {
        Duration warnAfter = mlProperties.polling().pendingWarnAfter();
        LocalDateTime startedAt = job.getStartedAt();
        if (startedAt == null || !startedAt.plus(warnAfter).isBefore(now)) {
            return;
        }
        LocalDateTime lastWarnedAt = pendingWarnedAt.get(job.getJobId());
        if (lastWarnedAt != null && !lastWarnedAt.plus(warnAfter).isBefore(now)) {
            return;
        }
        // pendingTimeout이 지난 기록은 이미 타임아웃 처리됐거나 취소 등으로 종료된 Job이다.
        LocalDateTime expired = now.minus(mlProperties.pendingTimeout());
        pendingWarnedAt.values().removeIf(warnedAt -> warnedAt.isBefore(expired));
        pendingWarnedAt.put(job.getJobId(), now);

        log.warn("[Polling] ML이 PENDING 응답 지속 - jobId: {}, mlJobId: {}, BE 상태: {}, 접수 후 {}분 경과 (ML 큐 대기·작업 유실 확인 필요)",
                job.getJobId(), job.getMlJobId(), job.getStatus(), Duration.between(startedAt, now).toMinutes());
    }

    /**
     * 연결 불가가 unreachableTimeout 넘게 이어졌는지. 연결 불가 시작 시각과 ML 접수 시각 중 늦은 쪽부터 잰다.
     */
    private boolean isUnreachableTimedOut(Job job, LocalDateTime now) {
        LocalDateTime since = mlConnectionState.getUnreachableSince();
        if (since == null || job.getStartedAt() == null) {
            return false;
        }
        LocalDateTime from = job.getStartedAt().isAfter(since) ? job.getStartedAt() : since;
        return from.plus(mlProperties.polling().unreachableTimeout()).isBefore(now);
    }

    private void markMlUnreachable(LocalDateTime now) {
        if (mlConnectionState.markUnreachable(now)) {
            log.warn("[Polling] ML 연결 불가 시작 - {} 넘게 이어지면 진행 중 Job을 실패 처리",
                    mlProperties.polling().unreachableTimeout());
        }
    }

    /**
     * ML 앱이 응답했으면(오류 응답 포함) 연결 불가 구간을 끝낸다.
     */
    private void markMlReachable(LocalDateTime now) {
        LocalDateTime since = mlConnectionState.markReachable();
        if (since != null) {
            log.info("[Polling] ML 연결 복구 - 연결 불가 {}분", Duration.between(since, now).toMinutes());
        }
    }

    private static String unavailableReason(BusinessException e) {
        return e instanceof MlUnavailableException unavailable ? unavailable.getReason() : null;
    }

    /**
     * ML artifacts의 결과 파일 하나를 확인하고 크기·형식을 미리 조회한다 (트랜잭션 밖).
     * 연결할 수 없으면 null을 반환하며, 이때 Job은 SUCCESS로 두고 해당 파일만 연결하지 않는다.
     * - key 없음(ML이 S3에 올리지 않음), 다른 버킷, 결과 경로(ml-results/) 밖의 key, S3에 객체 없음
     * 크기는 ML이 준 size_bytes를 우선 쓴다. S3 조회 자체가 실패(권한·네트워크)하면 일시 장애일 수 있어 형식 없이 연결한다.
     *
     * @param slot 로그용 파일 구분 (dataset / preview / model)
     */
    private ResultFileMeta describeResultFile(Job job, String slot, MlArtifact artifact) {
        if (artifact == null || !artifact.hasKey()) {
            log.warn("[Polling] 결과 파일 artifacts 없음 - jobId: {}, mlJobId: {}, file: {}",
                    job.getJobId(), job.getMlJobId(), slot);
            return null;
        }
        String rejectReason = artifact.rejectReason(bucketName);
        if (rejectReason != null) {
            log.error("[Polling] 결과 파일 연결 안 함({}) - jobId: {}, mlJobId: {}, file: {}, bucket: {}, key: {}",
                    rejectReason, job.getJobId(), job.getMlJobId(), slot, artifact.bucket(), artifact.key());
            return null;
        }
        S3ObjectLookup lookup = s3Service.headObject(artifact.key());
        if (lookup.notFound()) {
            log.error("[Polling] 결과 파일이 S3에 없음, 연결하지 않음 - jobId: {}, mlJobId: {}, file: {}, key: {}",
                    job.getJobId(), job.getMlJobId(), slot, artifact.key());
            return null;
        }
        S3ObjectInfo info = lookup.info();
        // 스토리지 사용량은 실제로 클라우드에 올라간 크기 기준이라 S3 조회 값을 먼저 쓴다
        Long size = info != null && info.size() != null ? info.size() : artifact.sizeBytes();
        return new ResultFileMeta(artifact.key(), size, info != null ? info.contentType() : null);
    }
}
