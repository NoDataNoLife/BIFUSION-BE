package com.nodatanolife.bifusion_be.domain.job.entity;

import com.nodatanolife.bifusion_be.domain.file.entity.File;
import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * 추론 이미지 1장의 분류 결과.
 * ML predictions[].sample_index는 요청에 보낸 이미지 순서이므로, 같은 순서의 입력 file과 짝지어 저장한다.
 */
@Entity
@Table(name = "inference_result")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InferenceResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inference_result_id")
    private Long inferenceResultId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inference_config_id", nullable = false)
    private InferenceConfig inferenceConfig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    @Column(name = "sample_index")
    private Integer sampleIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "predicted_class", length = 20)
    private PredictedClass predictedClass;

    @Column(name = "confidence", precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "normal_probability", precision = 5, scale = 4)
    private BigDecimal normalProbability;

    @Column(name = "anomaly_probability", precision = 5, scale = 4)
    private BigDecimal anomalyProbability;

    @Column(name = "actual_label", length = 20)
    private String actualLabel;

    @Builder
    private InferenceResult(InferenceConfig inferenceConfig, File file, Integer sampleIndex,
                            PredictedClass predictedClass, BigDecimal confidence,
                            BigDecimal normalProbability, BigDecimal anomalyProbability) {
        this.inferenceConfig = inferenceConfig;
        this.file = file;
        this.sampleIndex = sampleIndex;
        this.predictedClass = predictedClass;
        this.confidence = confidence;
        this.normalProbability = normalProbability;
        this.anomalyProbability = anomalyProbability;
    }

    public static InferenceResult create(InferenceConfig inferenceConfig, File file, int sampleIndex,
                                         PredictedClass predictedClass, BigDecimal confidence,
                                         BigDecimal normalProbability, BigDecimal anomalyProbability) {
        return InferenceResult.builder()
                .inferenceConfig(inferenceConfig)
                .file(file)
                .sampleIndex(sampleIndex)
                .predictedClass(predictedClass)
                .confidence(confidence)
                .normalProbability(normalProbability)
                .anomalyProbability(anomalyProbability)
                .build();
    }
}
