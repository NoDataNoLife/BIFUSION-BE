package com.nodatanolife.bifusion_be.domain.job.service;

import com.nodatanolife.bifusion_be.domain.job.entity.Job;
import com.nodatanolife.bifusion_be.domain.user.entity.RewardHistory;
import com.nodatanolife.bifusion_be.domain.user.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * AI 작업 실행 비용 차감·환불. 호출 측 트랜잭션(Job 생성, 실패·취소 반영) 안에서 Job 변경과 함께 처리한다.
 * 실패·취소는 종료 상태로의 전이라 Job마다 한 번만 일어나고(@Version), 환불도 그 전이와 같은 트랜잭션에서만 한다.
 */
@Component
@RequiredArgsConstructor
public class JobPointService {

    private final PointService pointService;

    /**
     * 실행한 사용자(재시도면 재시도한 사용자)에게서 차감한다. 잔액이 모자라면 400(INSUFFICIENT_POINTS)으로 Job 생성까지 롤백된다.
     */
    public void charge(Job job, int points) {
        pointService.spend(job.getUser().getUserId(), points, RewardHistory.TYPE_JOB_RUN,
                RewardHistory.TARGET_JOB, job.getJobId());
        job.recordCharge(points);
    }

    /**
     * 실패·취소된 Job의 차감액을 모두 돌려준다. 차감 도입 전 Job(chargedPoints null)은 돌려줄 것이 없다.
     */
    public void refund(Job job) {
        Integer chargedPoints = job.getChargedPoints();
        if (chargedPoints == null) {
            return;
        }
        pointService.earn(job.getUser().getUserId(), chargedPoints, RewardHistory.TYPE_JOB_REFUND,
                RewardHistory.TARGET_JOB, job.getJobId());
    }
}
