package com.nodatanolife.bifusion_be.domain.job.entity;

/**
 * BE Job 상태. FE가 그대로 인식하는 문자열이다.
 * SUCCESS / FAILED / CANCELLED는 종료 상태이며, 종료 후에는 어떤 전이도 허용하지 않는다.
 */
public enum JobStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELLED;

    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED || this == CANCELLED;
    }
}
