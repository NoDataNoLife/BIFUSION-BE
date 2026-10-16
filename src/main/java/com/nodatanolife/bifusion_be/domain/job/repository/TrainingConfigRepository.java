package com.nodatanolife.bifusion_be.domain.job.repository;

import com.nodatanolife.bifusion_be.domain.job.entity.TrainingConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrainingConfigRepository extends JpaRepository<TrainingConfig, Long> {

    Optional<TrainingConfig> findByJobJobId(Long jobId);
}
