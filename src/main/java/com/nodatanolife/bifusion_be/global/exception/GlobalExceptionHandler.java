package com.nodatanolife.bifusion_be.global.exception;

import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /* ================================
     * Business Exception
     * ================================ */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<CommonResponse<?>> handleBusinessException(
            BusinessException e
    ) {
        ErrorCode errorCode = e.getErrorCode();

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(CommonResponse.fail(
                        errorCode.getCode(),
                        errorCode.getMessage()
                ));
    }

    /* ================================
     * @Valid (RequestBody)
     * ================================ */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<CommonResponse<?>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e
    ) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;

        String message = e.getBindingResult()
                .getFieldErrors()
                .getFirst()
                .getDefaultMessage();

        return ResponseEntity
                .status(errorCode.getStatus()) // 400
                .body(CommonResponse.fail(
                        errorCode.getCode(),
                        message
                ));
    }

    /* ================================
     * @RequestParam / @PathVariable 검증
     * ================================ */
    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<CommonResponse<?>> handleConstraintViolation(
            ConstraintViolationException e
    ) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;

        return ResponseEntity
                .status(errorCode.getStatus()) // 400
                .body(CommonResponse.fail(
                        errorCode.getCode(),
                        e.getMessage()
                ));
    }

    /* ================================
     * 타입 불일치 (Long에 문자 등)
     * ================================ */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<CommonResponse<?>> handleTypeMismatch() {

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(CommonResponse.fail(
                        errorCode.getCode(),
                        "요청 값의 타입이 올바르지 않습니다."
                ));
    }

    /* ================================
     * 예상 못 한 예외
     * ================================ */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<CommonResponse<?>> handleException(Exception e) {
        log.error("[Unhandled Exception]", e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(CommonResponse.fail(
                        ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                        ErrorCode.INTERNAL_SERVER_ERROR.getMessage()
                ));
    }

}
