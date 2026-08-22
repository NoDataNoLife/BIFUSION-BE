package com.nodatanolife.bifusion_be.domain.job.entity;

import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "augmentation_sample_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AugmentationSampleImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "augmentation_sample_image_id")
    private Long augmentationSampleImageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "augmentation_config_id", nullable = false)
    private AugmentationConfig augmentationConfig;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "image_name")
    private String imageName;

    @Enumerated(EnumType.STRING)
    @Column(name = "label", length = 20)
    private SampleImageLabel label;

    @Column(name = "comment")
    private String comment;
}
