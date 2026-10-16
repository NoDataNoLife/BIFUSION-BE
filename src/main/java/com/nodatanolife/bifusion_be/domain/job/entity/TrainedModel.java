package com.nodatanolife.bifusion_be.domain.job.entity;

import com.nodatanolife.bifusion_be.domain.project.entity.Project;
import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 학습 작업으로 만들어진 분류 모델.
 * 모델 파일 본체는 RunPod Network Volume에 있고, 추론 요청 시 mlModelId를 ML에 전달한다.
 * 소유자는 원본 학습 Job의 user로 판단한다.
 */
@Entity
@Table(name = "trained_model")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainedModel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trained_model_id")
    private Long trainedModelId;

    // ML result.model_id
    @Column(name = "ml_model_id", length = 100, nullable = false)
    private String mlModelId;

    // 원본 학습 작업
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_job_id", nullable = false, unique = true)
    private Job trainingJob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "k_shot")
    private Integer kShot;

    // 다운로드용으로 ML이 우리 S3에 올린 모델 파일의 key
    @Column(name = "model_key")
    private String modelKey;

    @Builder
    private TrainedModel(String mlModelId, Job trainingJob, Project project, Integer kShot, String modelKey) {
        this.mlModelId = mlModelId;
        this.trainingJob = trainingJob;
        this.project = project;
        this.kShot = kShot;
        this.modelKey = modelKey;
    }

    public static TrainedModel create(Job trainingJob, String mlModelId, Integer kShot, String modelKey) {
        return TrainedModel.builder()
                .mlModelId(mlModelId)
                .trainingJob(trainingJob)
                .project(trainingJob.getProject())
                .kShot(kShot)
                .modelKey(modelKey)
                .build();
    }
}
