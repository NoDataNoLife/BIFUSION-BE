package com.nodatanolife.bifusion_be.domain.job.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "training_config")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainingConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "training_config_id")
    private Long trainingConfigId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "augmentation_config_id", nullable = false)
    private AugmentationConfig augmentationConfig;

    @Column(name = "k_shot")
    private Integer kShot;

    @Column(name = "has_query_label")
    private Boolean hasQueryLabel;

    @Column(name = "accuracy")
    private BigDecimal accuracy;

    @Column(name = "auroc")
    private BigDecimal auroc;

    @Column(name = "avg_confidence")
    private BigDecimal avgConfidence;

    @Column(name = "tp_count")
    private Integer tpCount;

    @Column(name = "fp_count")
    private Integer fpCount;

    @Column(name = "tn_count")
    private Integer tnCount;

    @Column(name = "fn_count")
    private Integer fnCount;

    @Column(name = "elapsed_time")
    private Integer elapsedTime;
}
