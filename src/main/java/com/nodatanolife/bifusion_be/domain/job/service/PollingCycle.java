package com.nodatanolife.bifusion_be.domain.job.service;

/**
 * 폴링 한 주기의 상태. ML 연결 실패가 한 번 나면 그 주기의 나머지 ML 조회는 건너뛴다.
 */
public class PollingCycle {

    private boolean mlUnavailable;
    private String mlUnavailableReason;
    private int skippedMlCalls;

    /**
     * @param reason 연결 실패 원인 (예: "HTTP 502", "ConnectException"). 주기 요약 로그용이며 처음 원인만 남긴다.
     */
    public void markMlUnavailable(String reason) {
        if (!mlUnavailable) {
            this.mlUnavailableReason = reason;
        }
        this.mlUnavailable = true;
    }

    public boolean isMlUnavailable() {
        return mlUnavailable;
    }

    public String getMlUnavailableReason() {
        return mlUnavailableReason;
    }

    public void skipMlCall() {
        this.skippedMlCalls++;
    }

    public int getSkippedMlCalls() {
        return skippedMlCalls;
    }
}
