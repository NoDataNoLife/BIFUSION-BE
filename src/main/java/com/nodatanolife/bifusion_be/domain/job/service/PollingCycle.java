package com.nodatanolife.bifusion_be.domain.job.service;

/**
 * 폴링 한 주기의 상태. ML 연결 실패가 한 번 나면 그 주기의 나머지 ML 조회는 건너뛴다.
 */
public class PollingCycle {

    private boolean mlUnavailable;
    private int skippedMlCalls;

    public void markMlUnavailable() {
        this.mlUnavailable = true;
    }

    public boolean isMlUnavailable() {
        return mlUnavailable;
    }

    public void skipMlCall() {
        this.skippedMlCalls++;
    }

    public int getSkippedMlCalls() {
        return skippedMlCalls;
    }
}
