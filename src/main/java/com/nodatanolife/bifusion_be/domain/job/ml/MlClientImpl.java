package com.nodatanolife.bifusion_be.domain.job.ml;

import com.nodatanolife.bifusion_be.domain.job.ml.dto.request.MlAugmentationRunRequest;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.request.MlInferenceRunRequest;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.request.MlTrainingRunRequest;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.response.MlAugmentationResult;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.response.MlInferenceResult;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.response.MlJobResponse;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.response.MlRunResponse;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.response.MlTrainingResult;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.function.Supplier;

/**
 * 실행 요청에는 Base64 이미지가 담기고 ML의 422 응답 본문은 입력값을 그대로 되돌려 주므로,
 * 요청·응답 본문은 어떤 경우에도 로그에 남기지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MlClientImpl implements MlClient {

    private static final String AUGMENTATION = "augmentation";
    private static final String TRAINING = "training";
    private static final String INFERENCE = "inference";

    private final RestClient mlRestClient;
    private final MlProperties mlProperties;

    @Override
    public String runAugmentation(MlAugmentationRunRequest request) {
        return run(AUGMENTATION, request);
    }

    @Override
    public String runTraining(MlTrainingRunRequest request) {
        return run(TRAINING, request);
    }

    @Override
    public String runInference(MlInferenceRunRequest request) {
        return run(INFERENCE, request);
    }

    @Override
    public MlJobResponse<MlAugmentationResult> getAugmentation(String mlJobId) {
        return get(AUGMENTATION, mlJobId, new ParameterizedTypeReference<>() {});
    }

    @Override
    public MlJobResponse<MlTrainingResult> getTraining(String mlJobId) {
        return get(TRAINING, mlJobId, new ParameterizedTypeReference<>() {});
    }

    @Override
    public MlJobResponse<MlInferenceResult> getInference(String mlJobId) {
        return get(INFERENCE, mlJobId, new ParameterizedTypeReference<>() {});
    }

    @Override
    public void cancelAugmentation(String mlJobId) {
        cancel(AUGMENTATION, mlJobId);
    }

    @Override
    public void cancelTraining(String mlJobId) {
        cancel(TRAINING, mlJobId);
    }

    @Override
    public void cancelInference(String mlJobId) {
        cancel(INFERENCE, mlJobId);
    }

    private String run(String task, Object request) {
        MlRunResponse response = execute(Operation.RUN, task, () -> mlRestClient.post()
                .uri("/api/v1/{task}/run", task)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(MlRunResponse.class));
        if (response == null || !StringUtils.hasText(response.jobId())) {
            log.error("[ML] 실행 응답에 job_id 없음 - task: {}", task);
            throw new BusinessException(ErrorCode.ML_RESPONSE_ERROR);
        }
        return response.jobId();
    }

    private <T> MlJobResponse<T> get(String task, String mlJobId,
                                     ParameterizedTypeReference<MlJobResponse<T>> type) {
        MlJobResponse<T> response = execute(Operation.GET, task, () -> mlRestClient.get()
                .uri("/api/v1/{task}/{jobId}", task, mlJobId)
                .retrieve()
                .body(type));
        if (response == null || response.status() == null) {
            log.error("[ML] 조회 응답에 status 없음 - task: {}, mlJobId: {}", task, mlJobId);
            throw new BusinessException(ErrorCode.ML_RESPONSE_ERROR);
        }
        return response;
    }

    private void cancel(String task, String mlJobId) {
        execute(Operation.CANCEL, task, () -> mlRestClient.post()
                .uri("/api/v1/{task}/{jobId}/cancel", task, mlJobId)
                .retrieve()
                .toBodilessEntity());
    }

    private <T> T execute(Operation operation, String task, Supplier<T> call) {
        if (!StringUtils.hasText(mlProperties.baseUrl())) {
            log.warn("[ML] base URL 미설정 - {} {}", operation, task);
            throw new BusinessException(ErrorCode.ML_CONNECTION_FAILED);
        }
        try {
            return call.get();
        } catch (RestClientResponseException e) {
            throw new BusinessException(classify(operation, task, e.getStatusCode()));
        } catch (ResourceAccessException e) {
            if (operation == Operation.RUN && isResponseTimeout(e)) {
                log.warn("[ML] 실행 요청 응답 대기 타임아웃 - ML 접수 여부 불확실: {}", task);
                throw new BusinessException(ErrorCode.ML_SUBMIT_TIMEOUT);
            }
            log.warn("[ML] 연결 실패 - {} {}: {}", operation, task, rootCauseName(e));
            throw new BusinessException(ErrorCode.ML_CONNECTION_FAILED);
        } catch (RestClientException e) {
            log.error("[ML] 응답 파싱 실패 - {} {}: {}", operation, task, e.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.ML_RESPONSE_ERROR);
        }
    }

    /**
     * 연결은 됐지만 응답을 기다리다 시간이 초과된 경우. 연결 타임아웃(HttpConnectTimeoutException)은 제외한다.
     */
    private static boolean isResponseTimeout(ResourceAccessException e) {
        Throwable cause = e.getCause();
        return cause instanceof HttpTimeoutException && !(cause instanceof HttpConnectTimeoutException);
    }

    private static String rootCauseName(Throwable e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        return cause.getClass().getSimpleName();
    }

    private ErrorCode classify(Operation operation, String task, HttpStatusCode status) {
        if (status.is5xxServerError()) {
            log.warn("[ML] 서버 오류 응답 - {} {}: {}", operation, task, status.value());
            return ErrorCode.ML_CONNECTION_FAILED;
        }
        if (operation == Operation.RUN && status.value() == HttpStatus.UNPROCESSABLE_ENTITY.value()) {
            log.error("[ML] 실행 요청 형식 오류(422) - BE 요청 DTO 확인 필요: {}", task);
            return ErrorCode.ML_REQUEST_INVALID;
        }
        if (operation != Operation.RUN && status.value() == HttpStatus.NOT_FOUND.value()) {
            return ErrorCode.ML_JOB_NOT_FOUND;
        }
        if (operation == Operation.CANCEL && status.value() == HttpStatus.CONFLICT.value()) {
            return ErrorCode.ML_JOB_ALREADY_FINISHED;
        }
        log.error("[ML] 예상하지 못한 응답 - {} {}: {}", operation, task, status.value());
        return ErrorCode.ML_RESPONSE_ERROR;
    }

    private enum Operation {
        RUN, GET, CANCEL
    }
}
