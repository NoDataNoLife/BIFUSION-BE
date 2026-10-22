package com.nodatanolife.bifusion_be.domain.job.ml;

import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.Getter;

/**
 * ML 연결 불가(ML_CONNECTION_FAILED). 폴링 주기 요약 로그에 원인을 남길 수 있도록 짧은 원인 설명을 함께 담는다.
 * 원인 설명에는 상태 코드·예외 이름만 넣고 응답 본문은 넣지 않는다.
 */
@Getter
public class MlUnavailableException extends BusinessException {

    /** 예: "HTTP 502", "ConnectException", "HttpTimeoutException", "base URL 미설정" */
    private final String reason;

    public MlUnavailableException(String reason) {
        super(ErrorCode.ML_CONNECTION_FAILED);
        this.reason = reason;
    }
}
