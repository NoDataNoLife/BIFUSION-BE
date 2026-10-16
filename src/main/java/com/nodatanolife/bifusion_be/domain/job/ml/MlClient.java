package com.nodatanolife.bifusion_be.domain.job.ml;

import com.nodatanolife.bifusion_be.domain.job.ml.dto.request.MlAugmentationRunRequest;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.request.MlInferenceRunRequest;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.request.MlTrainingRunRequest;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.response.MlAugmentationResult;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.response.MlInferenceResult;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.response.MlJobResponse;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.response.MlTrainingResult;

/**
 * ML 서버 호출 클라이언트.
 *
 * 오류는 BusinessException의 ErrorCode로 구분해 전달한다.
 * - ML_CONNECTION_FAILED : 연결 오류·타임아웃·5xx. 작업 실패가 아니므로 호출자가 재시도한다.
 * - ML_REQUEST_INVALID   : 실행 요청 422. BE 요청 형식 버그.
 * - ML_JOB_NOT_FOUND     : 조회·취소 404. ML이 모르는 작업.
 * - ML_JOB_ALREADY_FINISHED : 취소 409. 이미 종료된 작업.
 * - ML_RESPONSE_ERROR    : 그 외 4xx, 응답 파싱 실패.
 * ML이 정상 응답으로 FAILED 상태를 준 경우는 예외가 아니며 MlJobResponse.status로 전달된다.
 */
public interface MlClient {

    /** @return ML job_id */
    String runAugmentation(MlAugmentationRunRequest request);

    /** @return ML job_id */
    String runTraining(MlTrainingRunRequest request);

    /** @return ML job_id */
    String runInference(MlInferenceRunRequest request);

    MlJobResponse<MlAugmentationResult> getAugmentation(String mlJobId);

    MlJobResponse<MlTrainingResult> getTraining(String mlJobId);

    MlJobResponse<MlInferenceResult> getInference(String mlJobId);

    void cancelAugmentation(String mlJobId);

    void cancelTraining(String mlJobId);

    void cancelInference(String mlJobId);
}
