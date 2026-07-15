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
    ALREADY_ONBOARDED(HttpStatus.CONFLICT, "A007", "이미 온보딩이 완료된 사용자입니다."),
    DELETED_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U404", "삭제된 사용자를 찾을 수 없습니다."),

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
    EXPERT_ONLY(HttpStatus.FORBIDDEN, "QNA001", "전문가만 답변할 수 있습니다."),
    RECRUITMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CM005", "존재하지 않는 팀 모집글입니다."),
    ALREADY_APPLIED(HttpStatus.CONFLICT, "CM006", "이미 해당 공고에 지원한 이력이 있습니다."),
    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "CM007", "지원 내역을 찾을 수 없습니다."),
    APPLICATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CM008", "모집글 작성자만 처리할 수 있습니다."),
    INVALID_APPLICATION_STATUS(HttpStatus.BAD_REQUEST, "CM009", "허용되지 않은 상태값입니다."),
    RECRUITMENT_FULL(HttpStatus.CONFLICT, "CM010", "모집 정원이 마감되었습니다."),
    /* ================================
     * Collaboration / Project
     * ================================ */
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "프로젝트를 찾을 수 없습니다."),
    PROJECT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "P002", "프로젝트 접근 권한이 없습니다."),
    ALREADY_PROJECT_MEMBER(HttpStatus.CONFLICT, "P003", "이미 프로젝트 멤버입니다."),
    PROJECT_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "P004", "프로젝트 멤버를 찾을 수 없습니다."),
    COLLABORATION_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "P005", "협업 요청을 찾을 수 없습니다."),
    COLLABORATION_REQUEST_ALREADY_PROCESSED(HttpStatus.CONFLICT, "P006", "이미 처리된 협업 요청입니다."),
    PROJECT_VISIBILITY_PRIVATE_FORBIDDEN(HttpStatus.BAD_REQUEST, "P007", "비공개 프로젝트는 마이페이지에 공개로 설정할 수 없습니다."),
    PROJECT_LAST_MANAGER(HttpStatus.BAD_REQUEST, "P008", "관리자가 본인만 남은 경우 자기자신은 삭제 불가합니다."),
    PROJECT_SAME_ROLE(HttpStatus.BAD_REQUEST, "P009", "이미 동일한 역할입니다."),

    /* ================================
     * Invitation
     * ================================ */
    INVITATION_ALREADY_MEMBER(HttpStatus.CONFLICT, "PI001", "이미 프로젝트 멤버입니다."),
    INVITATION_COOLDOWN(HttpStatus.TOO_MANY_REQUESTS, "PI002", "초대 재발송은 1시간 후에 가능합니다."),
    INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "PI003", "초대를 찾을 수 없습니다."),
    INVITATION_EXPIRED(HttpStatus.BAD_REQUEST, "PI004", "만료된 초대입니다."),
    INVITATION_ALREADY_RESPONDED(HttpStatus.BAD_REQUEST, "PI005", "이미 처리된 초대입니다."),

    /* ================================
     * File
     * ================================ */
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F001", "파일 업로드 중 오류가 발생했습니다."),
    FILE_READ_FAILED(HttpStatus.BAD_REQUEST, "F002", "파일을 읽을 수 없습니다."),
    FILE_DOWNLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F003", "파일 다운로드 중 오류가 발생했습니다."),

    /* ================================
     * Expert Verification
     * ================================ */
    EXPERT_FILE_INVALID(HttpStatus.BAD_REQUEST, "EX001", "허용되지 않은 파일 형식입니다."),
    EXPERT_FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "EX002", "파일 용량이 허용 범위를 초과했습니다."),
    EXPERT_ALREADY_APPROVED(HttpStatus.CONFLICT, "EX003", "이미 전문가 인증이 완료된 사용자입니다."),

    /* ================================
     * Plan
     * ================================ */
    ALREADY_SUBSCRIBED_PLAN(HttpStatus.BAD_REQUEST, "U004", "이미 해당 플랜을 구독 중입니다."),
    PLAN_CHANGE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "U005", "플랜 변경 가능 기간이 아닙니다."),

    /* ================================
     * Activity (마이페이지 커뮤니티 활동)
     * ================================ */
    ACTIVITY_NOT_FOUND(HttpStatus.NOT_FOUND, "AC001", "활동 콘텐츠를 찾을 수 없습니다."),
    ACTIVITY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "AC002", "본인 소유의 활동만 변경할 수 있습니다."),
    ACTIVITY_PRIVATE_CONTENT_CANNOT_BE_PUBLIC(HttpStatus.BAD_REQUEST, "AC003", "비공개 콘텐츠는 마이페이지에 공개할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
