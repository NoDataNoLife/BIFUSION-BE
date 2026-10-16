package com.nodatanolife.bifusion_be.domain.job.repository;

import com.nodatanolife.bifusion_be.domain.job.entity.TrainedModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TrainedModelRepository extends JpaRepository<TrainedModel, Long> {

    Optional<TrainedModel> findByTrainingJobJobId(Long trainingJobId);

    /**
     * 프로젝트의 학습 모델 (학습 Job fetch join). 학습 완료 시각 최신순.
     */
    @Query("""
            select m from TrainedModel m
            join fetch m.trainingJob j
            where m.project.projectId = :projectId
            order by j.finishedAt desc, m.trainedModelId desc
            """)
    List<TrainedModel> findAllWithTrainingJobByProjectId(@Param("projectId") Long projectId);
}
