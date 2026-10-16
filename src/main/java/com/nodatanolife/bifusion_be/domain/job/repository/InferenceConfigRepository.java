package com.nodatanolife.bifusion_be.domain.job.repository;

import com.nodatanolife.bifusion_be.domain.job.entity.InferenceConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InferenceConfigRepository extends JpaRepository<InferenceConfig, Long> {

    Optional<InferenceConfig> findByJobJobId(Long jobId);
}
