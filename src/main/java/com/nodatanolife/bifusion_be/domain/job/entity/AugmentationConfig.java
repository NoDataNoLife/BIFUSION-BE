package com.nodatanolife.bifusion_be.domain.job.entity;

import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "augmentation_config")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AugmentationConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "augmentation_config_id")
    private Long augmentationConfigId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false, unique = true)
    private Job job;

    // TODO: 아래 예전 파라미터 컬럼(images_per_class ~ is_seed_fixed)은 검수 화면이 사용 중이라 유지. 검수 기능 정리 이슈에서 제거
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

    // 실제 ML에 보낸 파라미터 스냅샷
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parameters", columnDefinition = "jsonb")
    private AugmentationParameters parameters;

    // 입력 원본 fileId (요청 순서 그대로). 파일 연결이 해제돼도 기록으로 남는다.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "normal_file_ids", columnDefinition = "jsonb")
    private List<Long> normalFileIds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "anomaly_file_ids", columnDefinition = "jsonb")
    private List<Long> anomalyFileIds;

    // ML result.dataset_id. 데이터셋 본체는 RunPod Network Volume에 있고 학습 요청 시 이 값을 전달한다.
    @Column(name = "ml_dataset_id", length = 100)
    private String mlDatasetId;

    // 다운로드·미리보기용으로 ML이 우리 S3에 올린 파일의 key
    @Column(name = "dataset_key")
    private String datasetKey;

    @Column(name = "preview_key")
    private String previewKey;

    @Column(name = "generated_count")
    private Integer generatedCount;

    @Column(name = "total_sample_count")
    private Integer totalSampleCount;

    @Builder
    private AugmentationConfig(Job job, AugmentationParameters parameters,
                               List<Long> normalFileIds, List<Long> anomalyFileIds) {
        this.job = job;
        this.parameters = parameters;
        this.normalFileIds = List.copyOf(normalFileIds);
        this.anomalyFileIds = List.copyOf(anomalyFileIds);
        this.normalCount = normalFileIds.size();
        this.anomalyCount = anomalyFileIds.size();
    }

    public static AugmentationConfig create(Job job, AugmentationParameters parameters,
                                            List<Long> normalFileIds, List<Long> anomalyFileIds) {
        return AugmentationConfig.builder()
                .job(job)
                .parameters(parameters)
                .normalFileIds(normalFileIds)
                .anomalyFileIds(anomalyFileIds)
                .build();
    }

    /**
     * 학습 데이터로 쓸 수 있는 증강 결과인지. (증강 Job이 SUCCESS이고 ML 데이터셋 ID가 있음)
     * 학습 요청 검증과 증강 데이터셋 목록이 같은 기준을 쓰도록 이 메서드로만 판정한다.
     */
    public boolean isReadyForTraining() {
        return job.getStatus() == JobStatus.SUCCESS && mlDatasetId != null;
    }

    public void recordResult(String mlDatasetId, String datasetKey, String previewKey,
                             Integer generatedCount, Integer totalSampleCount) {
        this.mlDatasetId = mlDatasetId;
        this.datasetKey = datasetKey;
        this.previewKey = previewKey;
        this.generatedCount = generatedCount;
        this.totalSampleCount = totalSampleCount;
    }
}
