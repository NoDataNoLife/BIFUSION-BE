package com.nodatanolife.bifusion_be.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    /* ================================
     * Common
     * ================================ */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C001", "서버 오류가 발생했습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "C002", "잘못된 요청입니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C003", "입력값이 올바르지 않습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C004", "요청한 리소스를 찾을 수 없습니다."),

    /* ================================
     * Auth / User
     * ================================ */
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "A002", "접근 권한이 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "A003", "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "A004", "이미 사용 중인 이메일입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A005", "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "A006", "토큰이 만료되었습니다."),

    /* ================================
     * Bio Data / AI Augmentation
     * ================================ */
    BIODATA_NOT_FOUND(HttpStatus.NOT_FOUND, "B001", "바이오 데이터를 찾을 수 없습니다."),
    BIODATA_ACCESS_DENIED(HttpStatus.FORBIDDEN, "B002", "해당 바이오 데이터에 접근할 수 없습니다."),
    BIODATA_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "B003", "바이오 데이터 형식이 올바르지 않습니다."),
    AI_AUGMENTATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "B004", "AI 증강 처리에 실패했습니다."),
    AI_MODEL_NOT_AVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "B005", "현재 AI 모델을 사용할 수 없습니다."),
    AI_REQUEST_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "B006", "AI 요청 횟수를 초과했습니다."),

    /* ================================
     * Community
     * ================================ */
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "CM001", "게시글을 찾을 수 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CM002", "댓글을 찾을 수 없습니다."),
    COMMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CM003", "댓글에 대한 권한이 없습니다."),
    POST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CM004", "게시글에 대한 권한이 없습니다."),

    /* ================================
     * Collaboration / Project
     * ================================ */
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "프로젝트를 찾을 수 없습니다."),
    PROJECT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "P002", "프로젝트 접근 권한이 없습니다."),
    ALREADY_PROJECT_MEMBER(HttpStatus.CONFLICT, "P003", "이미 프로젝트 멤버입니다."),
    PROJECT_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "P004", "프로젝트 멤버를 찾을 수 없습니다."),
    COLLABORATION_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "P005", "협업 요청을 찾을 수 없습니다."),
    COLLABORATION_REQUEST_ALREADY_PROCESSED(HttpStatus.CONFLICT, "P006", "이미 처리된 협업 요청입니다."),

    /* ================================
     * File
     * ================================ */
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F001", "파일 업로드 중 오류가 발생했습니다."),
    FILE_READ_FAILED(HttpStatus.BAD_REQUEST, "F002", "파일을 읽을 수 없습니다."),
    FILE_DOWNLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F003", "파일 다운로드 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
