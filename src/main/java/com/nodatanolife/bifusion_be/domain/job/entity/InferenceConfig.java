package com.nodatanolife.bifusion_be.domain.job.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "\"InferenceConfig\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InferenceConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inference_config_id")
    private Long inferenceConfigId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_config_id", nullable = false)
    private TrainingConfig trainingConfig;

    @Column(name = "has_label")
    private Boolean hasLabel;

    @Column(name = "total_image_count")
    private Integer totalImageCount;

    @Column(name = "accuracy")
    private BigDecimal accuracy;

    @Column(name = "normal_count")
    private Integer normalCount;

    @Column(name = "anomaly_count")
    private Integer anomalyCount;

    @Column(name = "avg_confidence")
    private BigDecimal avgConfidence;

    @Column(name = "elapsed_time")
    private Integer elapsedTime;
}
