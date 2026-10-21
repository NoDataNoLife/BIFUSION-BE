package com.nodatanolife.bifusion_be.domain.job.entity;

import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Table(name = "training_config")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainingConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "training_config_id")
    private Long trainingConfigId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false, unique = true)
    private Job job;

    // 학습에 사용한 원본 증강 작업
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "augmentation_config_id", nullable = false)
    private AugmentationConfig augmentationConfig;

    @Column(name = "k_shot")
    private Integer kShot;

    // TODO: ML에서 제거된 개념(has_query_labels). 사용하지 않으며 정리 이슈에서 제거
    @Column(name = "has_query_label")
    private Boolean hasQueryLabel;

    @Column(name = "accuracy", precision = 5, scale = 4)
    private BigDecimal accuracy;

    @Column(name = "f1_score", precision = 5, scale = 4)
    private BigDecimal f1Score;

    @Column(name = "auroc", precision = 5, scale = 4)
    private BigDecimal auroc;

    @Column(name = "loss", precision = 10, scale = 4)
    private BigDecimal loss;

    @Column(name = "avg_confidence", precision = 5, scale = 4)
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

    // 실제 ML에 보낸 파라미터 스냅샷
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parameters", columnDefinition = "jsonb")
    private TrainingParameters parameters;

    @Builder
    private TrainingConfig(Job job, AugmentationConfig augmentationConfig, TrainingParameters parameters) {
        this.job = job;
        this.augmentationConfig = augmentationConfig;
        this.parameters = parameters;
        this.kShot = parameters.kShot();
    }

    public static TrainingConfig create(Job job, AugmentationConfig augmentationConfig, TrainingParameters parameters) {
        return TrainingConfig.builder()
                .job(job)
                .augmentationConfig(augmentationConfig)
                .parameters(parameters)
                .build();
    }

    public void recordMetrics(BigDecimal accuracy, BigDecimal f1Score, BigDecimal auroc, BigDecimal loss) {
        this.accuracy = accuracy;
        this.f1Score = f1Score;
        this.auroc = auroc;
        this.loss = loss;
    }
}
