package com.nodatanolife.bifusion_be.domain.user.service;

import com.nodatanolife.bifusion_be.domain.file.repository.FileRepository;
import com.nodatanolife.bifusion_be.domain.job.entity.JobStatus;
import com.nodatanolife.bifusion_be.domain.job.entity.JobType;
import com.nodatanolife.bifusion_be.domain.job.repository.JobRepository;
import com.nodatanolife.bifusion_be.domain.user.dto.response.ResourceUsageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * 대시보드 리소스 사용량 조회.
 * - 저장 용량: 소유한 파일(데이터셋 업로드·작업 입력·증강 결과·학습 모델)의 크기 합. 임시 업로드와 삭제된 데이터셋 파일은 제외.
 *   파일 크기는 업로드 시점 또는 결과 저장 시점에 S3 객체 크기로 기록한 값이다.
 * - 월간 증강: 이번 달(usage.timezone 기준)에 만든 증강 작업 수. 실패·취소는 제외하고 진행 중인 작업은 포함한다.
 * - 항목별로 집계에 실패하면 그 항목만 null로 응답하고 나머지는 그대로 내려준다 (0과 구분).
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class ResourceUsageServiceImpl implements ResourceUsageService {

    private static final List<JobStatus> NOT_COUNTED_STATUSES = List.of(JobStatus.FAILED, JobStatus.CANCELLED);

    private final FileRepository fileRepository;
    private final JobRepository jobRepository;
    private final ZoneId zone;

    public ResourceUsageServiceImpl(FileRepository fileRepository,
                                    JobRepository jobRepository,
                                    @Value("${usage.timezone}") String timezone) {
        this.fileRepository = fileRepository;
        this.jobRepository = jobRepository;
        this.zone = ZoneId.of(timezone);
    }

    @Override
    public ResourceUsageResponse getResourceUsage(Long userId) {
        ZonedDateTime periodStart = ZonedDateTime.now(zone).toLocalDate().withDayOfMonth(1).atStartOfDay(zone);
        ZonedDateTime periodEnd = periodStart.plusMonths(1);

        Long storageUsedBytes = null;
        Long unmeasuredFileCount = null;
        try {
            FileRepository.StorageUsage storage = fileRepository.sumStorageUsage(userId);
            storageUsedBytes = storage.getUsedBytes();
            unmeasuredFileCount = storage.getUnmeasuredCount();
        } catch (DataAccessException e) {
            log.error("[ResourceUsage] 저장 용량 집계 실패 - userId: {}", userId, e);
        }

        Long monthlyAugmentations = null;
        try {
            monthlyAugmentations = jobRepository.countCreatedBetween(userId, JobType.AUGMENTATION, NOT_COUNTED_STATUSES,
                    toServerTime(periodStart), toServerTime(periodEnd));
        } catch (DataAccessException e) {
            log.error("[ResourceUsage] 월간 증강 횟수 집계 실패 - userId: {}", userId, e);
        }

        return new ResourceUsageResponse(
                storageUsedBytes,
                // TODO: 저장 용량 한도 미정(고정 용량 정책 보류). 정해지면 설정값으로 채우고 업로드 시 한도 확인도 추가
                null,
                unmeasuredFileCount,
                monthlyAugmentations,
                // TODO: 월간 증강 횟수 한도 없음(작업 비용은 토큰 기준). 토큰 차감 정책 확정 후 횟수 한도가 필요한지 재검토
                null,
                periodStart.toOffsetDateTime(),
                periodEnd.toOffsetDateTime(),
                zone.getId()
        );
    }

    /**
     * createdAt은 서버 기본 타임존의 LocalDateTime으로 저장되므로(prod 컨테이너는 UTC), 기간 경계를 서버 시각으로 바꿔 비교한다.
     */
    private static LocalDateTime toServerTime(ZonedDateTime time) {
        return time.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }
}
