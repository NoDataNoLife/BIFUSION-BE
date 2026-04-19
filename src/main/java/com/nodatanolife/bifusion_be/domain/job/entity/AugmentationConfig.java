package com.nodatanolife.bifusion_be.domain.job.entity;

import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "augmentation_config")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AugmentationConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "augmentation_config_id")
    private Long augmentationConfigId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "images_per_class")
    private Integer imagesPerClass;

    @Column(name = "sampling_steps")
    private Integer samplingSteps;

    @Column(name = "guidance_scale")
    private BigDecimal guidanceScale;

    @Column(name = "is_seed_fixed")
    private Boolean isSeedFixed;

    @Column(name = "normal_count")
    private Integer normalCount;

    @Column(name = "anomaly_count")
    private Integer anomalyCount;

    @Column(name = "elapsed_time")
    private Integer elapsedTime;
}
