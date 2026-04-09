package com.nodatanolife.bifusion_be.domain.job.entity;

import com.nodatanolife.bifusion_be.domain.file.entity.File;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "inference_result")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InferenceResult {

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

    @Column(name = "predicted_class", length = 20)
    private String predictedClass;

    @Column(name = "confidence")
    private BigDecimal confidence;

    @Column(name = "actual_label", length = 20)
    private String actualLabel;
}
