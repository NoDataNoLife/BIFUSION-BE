package com.nodatanolife.bifusion_be.domain.job;

import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationConfig;
import com.nodatanolife.bifusion_be.domain.job.repository.AugmentationConfigRepository;
import com.nodatanolife.bifusion_be.domain.job.service.AugmentationResultZipper;
import com.nodatanolife.bifusion_be.domain.job.service.JobPollingService;
import com.nodatanolife.bifusion_be.domain.job.service.ResultFileMeta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 다운로드 zip(결과 npz + PNG 이미지) 도입 전에 끝난 증강 결과의 zip을 만드는 일회성 작업.
 * augmentation.zip-backfill.enabled=true로 한 번 띄운 뒤 다시 끈다.
 * 변환·업로드는 트랜잭션 밖에서 하고, 기록만 한 건씩 별도 트랜잭션으로 처리해 한 건이 실패해도 나머지는 계속한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "augmentation.zip-backfill.enabled", havingValue = "true")
public class AugmentationZipBackfillRunner implements ApplicationRunner {

    private final AugmentationConfigRepository augmentationConfigRepository;
    private final AugmentationResultZipper augmentationResultZipper;
    private final JobPollingService jobPollingService;

    @Override
    public void run(ApplicationArguments args) {
        List<AugmentationConfig> targets = augmentationConfigRepository.findAllWithoutDownloadZip();
        int filled = 0;
        int failed = 0;
        for (AugmentationConfig config : targets) {
            Long jobId = config.getJob().getJobId();
            try {
                ResultFileMeta zip = augmentationResultZipper.create(jobId, config.getJob().getDisplayCode(), config.getDatasetKey());
                if (zip != null && jobPollingService.recordDownloadZip(jobId, zip)) {
                    filled++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                failed++;
                log.warn("[AugmentationZipBackfill] 채우기 실패 - jobId: {}, err: {}", jobId, e.getClass().getSimpleName());
            }
        }
        log.info("[AugmentationZipBackfill] 대상 {}건 중 채움 {}건, 실패 {}건", targets.size(), filled, failed);
    }
}
