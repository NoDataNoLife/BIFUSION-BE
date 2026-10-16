package com.nodatanolife.bifusion_be.domain.job.repository;

import com.nodatanolife.bifusion_be.domain.job.entity.TrainingConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TrainingConfigRepository extends JpaRepository<TrainingConfig, Long> {

    Optional<TrainingConfig> findByJobJobId(Long jobId);

    /**
     * 학습 Job id 목록으로 학습 설정 조회 (원본 증강 Config·Job fetch join).
     */
    @Query("""
            select tc from TrainingConfig tc
            join fetch tc.augmentationConfig ac
            join fetch ac.job
            where tc.job.jobId in :jobIds
            """)
    List<TrainingConfig> findAllWithSourceByJobIdIn(@Param("jobIds") Collection<Long> jobIds);
}
