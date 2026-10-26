package com.nodatanolife.bifusion_be.domain.expert;

import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequestStatus;
import com.nodatanolife.bifusion_be.domain.expert.repository.InspectionRequestRepository;
import com.nodatanolife.bifusion_be.domain.expert.service.InspectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 전문가 자동 배정 도입 전에 만들어져 배정 없이 대기 중으로 남은 검수 신청을 취소·환불하는 일회성 작업.
 * inspection.unassigned-cleanup.enabled=true로 한 번 띄운 뒤 다시 끈다.
 * 한 건씩 별도 트랜잭션으로 처리해, 한 건이 실패해도 나머지는 계속한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "inspection.unassigned-cleanup.enabled", havingValue = "true")
public class UnassignedInspectionCleanupRunner implements ApplicationRunner {

    private final InspectionRequestRepository inspectionRequestRepository;
    private final InspectionService inspectionService;

    @Override
    public void run(ApplicationArguments args) {
        List<Long> ids = inspectionRequestRepository.findIdsWithoutTaskByStatus(InspectionRequestStatus.PENDING);
        int cancelled = 0;
        int failed = 0;
        for (Long id : ids) {
            try {
                if (inspectionService.cancelUnassigned(id)) {
                    cancelled++;
                }
            } catch (Exception e) {
                failed++;
                log.warn("[UnassignedInspectionCleanup] 취소 실패 - inspectionRequestId: {}, err: {}", id, e.getClass().getSimpleName());
            }
        }
        log.info("[UnassignedInspectionCleanup] 대상 {}건 중 취소·환불 {}건, 실패 {}건", ids.size(), cancelled, failed);
    }
}
