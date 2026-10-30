package com.nodatanolife.bifusion_be.domain.job.repository;

import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationConfig;
import com.nodatanolife.bifusion_be.domain.job.entity.JobStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AugmentationConfigRepository extends JpaRepository<AugmentationConfig, Long> {

    Optional<AugmentationConfig> findByJobJobId(Long jobId);

    // 다운로드 zip 기록용. 같은 결과의 zip을 동시에 기록해도 File 행이 한 번만 생기도록 행을 잠근다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ac from AugmentationConfig ac where ac.job.jobId = :jobId")
    Optional<AugmentationConfig> findByJobIdForUpdate(@Param("jobId") Long jobId);

    List<AugmentationConfig> findAllByJobProjectProjectId(Long projectId);

    // 다운로드 zip 도입 전에 끝나 zip이 없는 증강 결과 (일회성 채우기용)
    @Query("""
            select ac from AugmentationConfig ac
            join fetch ac.job j
            where j.status = com.nodatanolife.bifusion_be.domain.job.entity.JobStatus.SUCCESS
              and ac.deletedAt is null and ac.datasetKey is not null and ac.downloadZipKey is null
            """)
    List<AugmentationConfig> findAllWithoutDownloadZip();

    /**
     * 프로젝트의 특정 상태 증강 결과 (Job fetch join). 완료 시각 최신순.
     */
    @Query("""
            select ac from AugmentationConfig ac
            join fetch ac.job j
            where j.project.projectId = :projectId and j.status = :status
            order by j.finishedAt desc, j.jobId desc
            """)
    List<AugmentationConfig> findAllWithJobByProjectIdAndStatus(@Param("projectId") Long projectId,
                                                                @Param("status") JobStatus status);
}
