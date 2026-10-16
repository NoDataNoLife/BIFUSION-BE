package com.nodatanolife.bifusion_be.domain.job.repository;

import com.nodatanolife.bifusion_be.domain.job.entity.TrainedModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrainedModelRepository extends JpaRepository<TrainedModel, Long> {

    Optional<TrainedModel> findByTrainingJobJobId(Long trainingJobId);
}
