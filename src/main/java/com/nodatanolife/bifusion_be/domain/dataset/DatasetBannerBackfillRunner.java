package com.nodatanolife.bifusion_be.domain.dataset;

import com.nodatanolife.bifusion_be.domain.dataset.repository.DatasetRepository;
import com.nodatanolife.bifusion_be.domain.dataset.service.DatasetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 대표 이미지 추출 기능 도입 전에 등록된 업로드 데이터셋의 대표 이미지를 채우는 일회성 작업.
 * dataset.banner-backfill.enabled=true로 한 번 띄운 뒤 다시 끈다.
 * 한 건씩 별도 트랜잭션으로 처리해, 한 건이 실패해도 나머지는 계속한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "dataset.banner-backfill.enabled", havingValue = "true")
public class DatasetBannerBackfillRunner implements ApplicationRunner {

    private final DatasetRepository datasetRepository;
    private final DatasetService datasetService;

    @Override
    public void run(ApplicationArguments args) {
        List<Long> ids = datasetRepository.findIdsWithoutBannerImage();
        int filled = 0;
        int failed = 0;
        for (Long id : ids) {
            try {
                if (datasetService.backfillBannerImage(id)) {
                    filled++;
                }
            } catch (Exception e) {
                failed++;
                log.warn("[DatasetBannerBackfill] 채우기 실패 - datasetId: {}, err: {}", id, e.getClass().getSimpleName());
            }
        }
        log.info("[DatasetBannerBackfill] 대상 {}건 중 채움 {}건, 이미지 없음 {}건, 실패 {}건",
                ids.size(), filled, ids.size() - filled - failed, failed);
    }
}
