package com.nodatanolife.bifusion_be.domain.job.entity;

import com.nodatanolife.bifusion_be.domain.file.entity.File;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "top3_support")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Top3Support {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "top3_support_id")
    private Long top3SupportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inference_result_id", nullable = false)
    private InferenceResult inferenceResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    @Column(name = "rank")
    private Integer rank;
}
