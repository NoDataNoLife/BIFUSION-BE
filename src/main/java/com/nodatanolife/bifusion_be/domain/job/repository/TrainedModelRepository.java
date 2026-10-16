package com.nodatanolife.bifusion_be.domain.job.repository;

import com.nodatanolife.bifusion_be.domain.job.entity.TrainedModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainedModelRepository extends JpaRepository<TrainedModel, Long> {
}
