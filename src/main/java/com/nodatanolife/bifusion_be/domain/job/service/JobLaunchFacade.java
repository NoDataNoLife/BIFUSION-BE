package com.nodatanolife.bifusion_be.domain.job.service;

import com.nodatanolife.bifusion_be.domain.job.dto.request.JobCreateRequest;
import com.nodatanolife.bifusion_be.domain.job.dto.response.JobResponse;
import com.nodatanolife.bifusion_be.domain.job.entity.JobFailureReason;
import com.nodatanolife.bifusion_be.domain.job.ml.MlClient;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.request.MlAugmentationRunRequest;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.request.MlInferenceRunRequest;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.request.MlTrainingRunRequest;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * AI Job 생성 흐름 조율. ML 요청은 수십 MB·최대 60초가 걸리므로 DB 트랜잭션 밖에서 호출한다.
 *
 * 1) 트랜잭션 1: 검증 + PENDING Job 생성 + 입력 파일 연결   (JobCommandService.createPending)
 * 2) 트랜잭션 밖: S3 → Base64 변환, ML 실행 요청
 * 3) 트랜잭션 2: 접수 성공이면 start, 실패면 fail (markStarted / markFailed). 실패해도 입력 파일은 Job에 보존한다.
 *
 * 이 클래스 자체는 트랜잭션을 갖지 않는다. 트랜잭션 메서드는 JobCommandService에만 있다.
 * ML 자동 재시도는 하지 않는다 (GPU 작업 중복 위험).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobLaunchFacade {

    private final JobCommandService jobCommandService;
    private final JobQueryService jobQueryService;
    private final MlImageEncodeService mlImageEncodeService;
    private final MlClient mlClient;

    public JobResponse launch(Long userId, Long projectId, JobCreateRequest request, String idempotencyKey) {
        return execute(userId, idempotencyKey,
                key -> jobCommandService.createPending(userId, projectId, request, key));
    }

    /**
     * 실패·취소된 Job을 같은 입력으로 다시 실행한다. 응답·실패 처리(503 등)는 생성과 같다.
     */
    public JobResponse retry(Long userId, Long jobId, String idempotencyKey) {
        return execute(userId, idempotencyKey,
                key -> jobCommandService.createRetry(userId, jobId, key));
    }

    /**
     * 실행 계획 생성(트랜잭션 1) → ML 실행 요청(트랜잭션 밖) → 결과 반영(트랜잭션 2) → 응답.
     * 생성과 재시도는 실행 계획을 만드는 방법만 다르고 이 흐름을 그대로 공유한다.
     *
     * @param planner 정규화된 멱등 키를 받아 트랜잭션 1을 수행하고 실행 계획을 돌려준다.
     */
    private JobResponse execute(Long userId, String idempotencyKey, Function<String, JobLaunchPlan> planner) {
        String key = StringUtils.hasText(idempotencyKey) ? idempotencyKey.strip() : null;

        JobLaunchPlan plan;
        try {
            plan = planner.apply(key);
        } catch (DataIntegrityViolationException e) {
            // 같은 키로 동시에 들어온 요청: 먼저 커밋된 Job을 돌려준다.
            if (key == null) {
                throw e;
            }
            return jobQueryService.getJobByIdempotencyKey(userId, key);
        }

        if (!plan.existing()) {
            submit(userId, plan);
        }
        return jobQueryService.getJob(userId, plan.jobId());
    }

    private void submit(Long userId, JobLaunchPlan plan) {
        try {
            String mlJobId = runOnMl(userId, plan);
            jobCommandService.markStarted(plan.jobId(), mlJobId);
        } catch (BusinessException e) {
            handleSubmitFailure(plan.jobId(), e);
        } catch (RuntimeException e) {
            log.error("[Job] ML 실행 요청 중 예상하지 못한 오류 - jobId: {}", plan.jobId(), e);
            jobCommandService.markFailed(plan.jobId(), JobFailureReason.UNKNOWN);
            throw e;
        }
    }

    private String runOnMl(Long userId, JobLaunchPlan plan) {
        return switch (plan.type()) {
            case AUGMENTATION -> {
                // 정상·비정상을 합쳐 한 번에 변환해야 총 용량 상한이 ML 요청 단위로 적용된다.
                Map<Long, String> encoded = encode(userId, plan,
                        Stream.concat(plan.normalFileIds().stream(), plan.anomalyFileIds().stream()).toList());
                yield mlClient.runAugmentation(MlAugmentationRunRequest.from(plan.augmentationParameters(),
                        pick(encoded, plan.normalFileIds()), pick(encoded, plan.anomalyFileIds())));
            }
            case TRAINING -> mlClient.runTraining(
                    MlTrainingRunRequest.from(plan.mlDatasetId(), plan.trainingParameters()));
            case INFERENCE -> {
                Map<Long, String> encoded = encode(userId, plan, plan.inferenceFileIds());
                yield mlClient.runInference(
                        MlInferenceRunRequest.from(plan.mlModelId(), pick(encoded, plan.inferenceFileIds())));
            }
        };
    }

    /**
     * 실패 사유를 Job에 기록하고 FE 응답을 결정한다. 입력 파일은 Job에 연결된 채로 보존된다.
     * ML 요청 형식 오류(422)만 실패 상태의 Job을 정상 응답하고, 나머지는 원래 ErrorCode로 응답한다.
     * 에러 응답으로 실패를 바로 알 수 있으므로 알림은 422일 때만 보낸다.
     */
    private void handleSubmitFailure(Long jobId, BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        switch (errorCode) {
            case UNSUPPORTED_IMAGE_FORMAT -> {
                jobCommandService.markFailed(jobId, JobFailureReason.INVALID_IMAGE);
                throw e;
            }
            case ML_REQUEST_INVALID -> {
                log.error("[Job] ML 실행 요청 형식 오류(422) - BE 요청 DTO 확인 필요, jobId: {}", jobId);
                // 에러 응답 없이 FAILED Job이 정상 응답으로 나가므로 이 경우만 실패 알림을 보낸다.
                jobCommandService.markFailedAndNotify(jobId, JobFailureReason.UNKNOWN);
            }
            case ML_CONNECTION_FAILED -> {
                jobCommandService.markFailed(jobId, JobFailureReason.ML_UNAVAILABLE);
                throw e;
            }
            case ML_SUBMIT_TIMEOUT -> {
                log.warn("[Job] ML 실행 요청 응답 지연 - 접수 여부 불확실, jobId: {}", jobId);
                jobCommandService.markFailed(jobId, JobFailureReason.ML_SUBMIT_UNCERTAIN);
                throw e;
            }
            default -> {
                // S3 객체 없음, 실제 용량 초과, ML 응답 오류 등
                log.error("[Job] ML 실행 단계 실패 - jobId: {}, code: {}", jobId, errorCode);
                jobCommandService.markFailed(jobId, JobFailureReason.UNKNOWN);
                throw e;
            }
        }
    }

    /**
     * 생성은 파일 소유자 기준, 재시도는 "이 Job의 입력 파일인지" 기준으로 확인한 뒤 변환한다.
     */
    private Map<Long, String> encode(Long userId, JobLaunchPlan plan, List<Long> fileIds) {
        return plan.retry()
                ? mlImageEncodeService.encodeJobInputs(plan.jobId(), fileIds)
                : mlImageEncodeService.encodeToBase64(userId, fileIds);
    }

    private static List<String> pick(Map<Long, String> encoded, List<Long> fileIds) {
        return fileIds.stream().map(encoded::get).toList();
    }
}
