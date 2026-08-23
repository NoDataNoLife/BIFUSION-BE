package com.nodatanolife.bifusion_be.domain.job.repository;

import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationConfig;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationSampleImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AugmentationSampleImageRepository extends JpaRepository<AugmentationSampleImage, Long> {

    List<AugmentationSampleImage> findAllByAugmentationConfigOrderByAugmentationSampleImageIdAsc(AugmentationConfig augmentationConfig);

    Optional<AugmentationSampleImage> findByAugmentationSampleImageIdAndAugmentationConfig(Long augmentationSampleImageId, AugmentationConfig augmentationConfig);
}
