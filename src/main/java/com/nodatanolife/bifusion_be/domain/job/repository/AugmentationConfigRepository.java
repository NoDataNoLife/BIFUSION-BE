package com.nodatanolife.bifusion_be.domain.job.repository;

import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationConfig;
import com.nodatanolife.bifusion_be.domain.job.entity.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AugmentationConfigRepository extends JpaRepository<AugmentationConfig, Long> {

    Optional<AugmentationConfig> findByJobJobId(Long jobId);

    List<AugmentationConfig> findAllByJobProjectProjectId(Long projectId);

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
