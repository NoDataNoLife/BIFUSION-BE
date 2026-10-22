package com.nodatanolife.bifusion_be.domain.job;

import com.nodatanolife.bifusion_be.domain.job.entity.Job;
import com.nodatanolife.bifusion_be.domain.job.service.JobPollingService;
import com.nodatanolife.bifusion_be.domain.job.service.MlJobPoller;
import com.nodatanolife.bifusion_be.domain.job.service.PollingCycle;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ML 작업 상태 폴링. PENDING·RUNNING Job을 ML에 조회해 상태·결과를 DB에 반영한다.
 * FE는 BE 조회 API(DB 값)만 폴링하고, ML 폴링은 이 스케줄러만 한다.
 *
 * - fixedDelay: 이전 실행이 끝나야 다음 실행이 시작되므로 실행이 겹치지 않는다.
 * - Job 하나에서 예외가 나도 나머지 Job 처리는 계속한다.
 * - 서버 1대 기준. 여러 대로 늘리면 분산 락(ShedLock 등)이 필요하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MlJobPollingScheduler {

    private final JobPollingService jobPollingService;
    private final MlJobPoller mlJobPoller;

    @Scheduled(fixedDelayString = "${ml.polling.interval}")
    public void pollMlJobs() {
        List<Job> targets;
        try {
            targets = jobPollingService.findPollTargets();
        } catch (RuntimeException e) {
            log.error("[Polling] 폴링 대상 조회 실패", e);
            return;
        }

        PollingCycle cycle = new PollingCycle();
        for (Job job : targets) {
            try {
                mlJobPoller.poll(job, cycle);
            } catch (ObjectOptimisticLockingFailureException e) {
                // 취소 등 다른 요청과 동시에 변경됨. 다음 주기에 다시 확인한다.
                log.info("[Polling] 동시 변경 감지 - 다음 주기에 다시 확인, jobId: {}", job.getJobId());
            } catch (BusinessException e) {
                log.warn("[Polling] Job 반영 실패 - jobId: {}, code: {}", job.getJobId(), e.getErrorCode());
            } catch (RuntimeException e) {
                log.error("[Polling] Job 처리 중 예외 - jobId: {}", job.getJobId(), e);
            }
        }

        if (cycle.isMlUnavailable()) {
            log.warn("[Polling] ML 서버 연결 실패({}) - 이번 주기 나머지 ML 조회 {}건 건너뜀, 다음 주기에 재시도",
                    cycle.getMlUnavailableReason(), cycle.getSkippedMlCalls());
        }
    }
}
