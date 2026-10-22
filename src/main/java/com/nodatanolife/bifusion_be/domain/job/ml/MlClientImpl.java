package com.nodatanolife.bifusion_be.domain.job.ml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 요청·응답 본문은 어떤 경우에도 로그에 남기지 않는다. 응답 파싱 실패 시에는 실패한 필드 경로와 짧은 원인만 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MlClientImpl implements MlClient {

    private static final String AUGMENTATION = "augmentation";
    private static final String TRAINING = "training";
    private static final String INFERENCE = "inference";
    private static final String JOB_NOT_FOUND_CODE = "JOB_NOT_FOUND";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int PARSE_FAILURE_MESSAGE_MAX_LENGTH = 300;

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
        MlRunResponse response = execute(Operation.RUN, task, null, () -> mlRestClient.post()
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
        MlJobResponse<T> response = execute(Operation.GET, task, mlJobId, () -> mlRestClient.get()
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
        execute(Operation.CANCEL, task, mlJobId, () -> mlRestClient.post()
                .uri("/api/v1/{task}/{jobId}/cancel", task, mlJobId)
                .retrieve()
                .toBodilessEntity());
    }

    /**
     * @param mlJobId 로그용. 실행 요청(RUN)은 아직 없으므로 null
     */
    private <T> T execute(Operation operation, String task, String mlJobId, Supplier<T> call) {
        if (!StringUtils.hasText(mlProperties.baseUrl())) {
            log.warn("[ML] base URL 미설정 - {} {}", operation, task);
            throw new MlUnavailableException("base URL 미설정");
        }
        try {
            return call.get();
        } catch (RestClientResponseException e) {
            ErrorCode errorCode = classify(operation, task, e);
            if (errorCode == ErrorCode.ML_CONNECTION_FAILED) {
                throw new MlUnavailableException("HTTP " + e.getStatusCode().value());
            }
            throw new BusinessException(errorCode);
        } catch (ResourceAccessException e) {
            if (operation == Operation.RUN && isResponseTimeout(e)) {
                log.warn("[ML] 실행 요청 응답 대기 타임아웃 - ML 접수 여부 불확실: {}", task);
                throw new BusinessException(ErrorCode.ML_SUBMIT_TIMEOUT);
            }
            String cause = rootCauseName(e);
            logUnavailable(operation, "[ML] 연결 실패 - {} {}: {}", operation, task, cause);
            throw new MlUnavailableException(cause);
        } catch (RestClientException e) {
            log.error("[ML] 응답 파싱 실패 - {} {}, mlJobId: {}, 원인: {}",
                    operation, task, mlJobId, describeParseFailure(e));
            throw new BusinessException(ErrorCode.ML_RESPONSE_ERROR);
        }
    }

    /**
     * 파싱 실패 원인을 짧게 설명한다. Jackson 오류면 실패한 필드 경로와 위치 정보를 뺀 메시지를 남긴다
     * (enum 값 불일치 등은 거부된 값 하나가 메시지에 포함된다). 그 밖의 예외는 메시지에 본문이 섞일 수 있어 클래스명만 남긴다.
     */
    private static String describeParseFailure(RestClientException e) {
        Throwable cause = e.getMostSpecificCause();
        if (cause instanceof JsonMappingException mapping) {
            return mapping.getClass().getSimpleName() + " at " + mapping.getPathReference()
                    + ": " + truncate(mapping.getOriginalMessage());
        }
        if (cause instanceof JsonProcessingException processing) {
            return processing.getClass().getSimpleName() + ": " + truncate(processing.getOriginalMessage());
        }
        return cause.getClass().getSimpleName();
    }

    private static String truncate(String message) {
        if (message == null || message.length() <= PARSE_FAILURE_MESSAGE_MAX_LENGTH) {
            return message;
        }
        return message.substring(0, PARSE_FAILURE_MESSAGE_MAX_LENGTH) + "...";
    }

    /**
     * ML 연결 불가 로그. 조회(GET)는 폴링 스케줄러가 주기마다 한 줄로 요약하므로 호출마다 남기지 않는다(debug).
     */
    private static void logUnavailable(Operation operation, String format, Object... args) {
        if (operation == Operation.GET) {
            log.debug(format, args);
        } else {
            log.warn(format, args);
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

    private ErrorCode classify(Operation operation, String task, RestClientResponseException e) {
        HttpStatusCode status = e.getStatusCode();
        if (status.is5xxServerError()) {
            logUnavailable(operation, "[ML] 서버 오류 응답 - {} {}: {}", operation, task, status.value());
            return ErrorCode.ML_CONNECTION_FAILED;
        }
        if (operation == Operation.RUN && status.value() == HttpStatus.UNPROCESSABLE_ENTITY.value()) {
            log.error("[ML] 실행 요청 형식 오류(422) - BE 요청 DTO 확인 필요: {}", task);
            return ErrorCode.ML_REQUEST_INVALID;
        }
        if (operation != Operation.RUN && status.value() == HttpStatus.NOT_FOUND.value()) {
            if (isJobNotFound(e)) {
                return ErrorCode.ML_JOB_NOT_FOUND;
            }
            // 경로 없음(ML 주소 오설정·엔드포인트 미구현 등)일 수 있어 작업 없음으로 보지 않는다. 호출자는 상태를 바꾸지 않는다.
            logUnavailable(operation, "[ML] 404 응답에 error_code JOB_NOT_FOUND 없음 - 작업 없음으로 보지 않음, {} {}",
                    operation, task);
            return ErrorCode.ML_RESPONSE_ERROR;
        }
        if (operation == Operation.CANCEL && status.value() == HttpStatus.CONFLICT.value()) {
            return ErrorCode.ML_JOB_ALREADY_FINISHED;
        }
        log.error("[ML] 예상하지 못한 응답 - {} {}: {}", operation, task, status.value());
        return ErrorCode.ML_RESPONSE_ERROR;
    }

    /**
     * 404 응답 본문이 ML의 "작업 없음" 형식인지 확인한다: {"detail": {"error_code": "JOB_NOT_FOUND", ...}}
     * FastAPI는 경로가 없을 때도 404({"detail": "Not Found"})를 주므로 상태 코드만으로는 구분할 수 없다.
     * 본문이 없거나 파싱에 실패하면 false. 본문은 로그에 남기지 않는다.
     */
    private static boolean isJobNotFound(RestClientResponseException e) {
        try {
            JsonNode errorCode = OBJECT_MAPPER.readTree(e.getResponseBodyAsString()).path("detail").path("error_code");
            return JOB_NOT_FOUND_CODE.equals(errorCode.asText(null));
        } catch (Exception parseFailure) {
            return false;
        }
    }

    private enum Operation {
        RUN, GET, CANCEL
    }
}
