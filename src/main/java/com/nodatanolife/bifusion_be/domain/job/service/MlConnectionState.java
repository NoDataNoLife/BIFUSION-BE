package com.nodatanolife.bifusion_be.domain.job.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 폴링으로 확인한 ML 연결 상태. 폴러가 갱신하고, Job 조회 응답이 "AI 서버 연결 지연" 안내용으로 읽는다.
 * 메모리에만 두므로 서버 재시작 시 초기화되고, BE 인스턴스마다 따로 판정한다.
 */
@Component
public class MlConnectionState {

    // 폴링 조회로 ML 연결 불가가 처음 확인된 시각. ML 앱이 응답하면 null.
    private volatile LocalDateTime unreachableSince;

    public LocalDateTime getUnreachableSince() {
        return unreachableSince;
    }

    /**
     * @return 이번 호출로 연결 불가 구간이 시작됐으면 true (이미 연결 불가였으면 false)
     */
    public synchronized boolean markUnreachable(LocalDateTime now) {
        if (unreachableSince != null) {
            return false;
        }
        unreachableSince = now;
        return true;
    }

    /**
     * @return 끝난 연결 불가 구간의 시작 시각. 연결 불가가 아니었으면 null
     */
    public synchronized LocalDateTime markReachable() {
        LocalDateTime since = unreachableSince;
        unreachableSince = null;
        return since;
    }
}
