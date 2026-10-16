package com.nodatanolife.bifusion_be.domain.job.repository;

import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AugmentationConfigRepository extends JpaRepository<AugmentationConfig, Long> {

    Optional<AugmentationConfig> findByJobJobId(Long jobId);
}
