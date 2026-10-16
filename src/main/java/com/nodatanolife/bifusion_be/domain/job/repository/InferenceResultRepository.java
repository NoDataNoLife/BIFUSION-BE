package com.nodatanolife.bifusion_be.domain.job.repository;

import com.nodatanolife.bifusion_be.domain.job.entity.InferenceResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InferenceResultRepository extends JpaRepository<InferenceResult, Long> {

    List<InferenceResult> findAllByInferenceConfigJobJobIdOrderBySampleIndexAsc(Long jobId);
}
