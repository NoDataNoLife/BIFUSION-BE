package com.nodatanolife.bifusion_be.domain.job.entity;

import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "\"InferenceConfig\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InferenceConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inference_config_id")
    private Long inferenceConfigId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false, unique = true)
    private Job job;

    // 추론에 사용한 학습 모델
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trained_model_id", nullable = false)
    private TrainedModel trainedModel;

    @Column(name = "has_label")
    private Boolean hasLabel;

    @Column(name = "total_image_count")
    private Integer totalImageCount;

    @Column(name = "accuracy", precision = 5, scale = 4)
    private BigDecimal accuracy;

    @Column(name = "normal_count")
    private Integer normalCount;

    @Column(name = "anomaly_count")
    private Integer anomalyCount;

    @Column(name = "avg_confidence", precision = 5, scale = 4)
    private BigDecimal avgConfidence;

    @Column(name = "elapsed_time")
    private Integer elapsedTime;

    @Builder
    private InferenceConfig(Job job, TrainedModel trainedModel, Integer totalImageCount) {
        this.job = job;
        this.trainedModel = trainedModel;
        this.totalImageCount = totalImageCount;
    }

    public static InferenceConfig create(Job job, TrainedModel trainedModel, int totalImageCount) {
        return InferenceConfig.builder()
                .job(job)
                .trainedModel(trainedModel)
                .totalImageCount(totalImageCount)
                .build();
    }

    public void recordSummary(int normalCount, int anomalyCount, BigDecimal avgConfidence) {
        this.normalCount = normalCount;
        this.anomalyCount = anomalyCount;
        this.avgConfidence = avgConfidence;
    }
}
