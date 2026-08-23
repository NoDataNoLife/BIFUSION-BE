package com.nodatanolife.bifusion_be.domain.expert.dto.response;

import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationSampleImage;

import java.time.LocalDateTime;

public record InspectionImageCommentResponse(
        Long imageId,
        String comment,
        LocalDateTime updatedAt
) {
    public static InspectionImageCommentResponse from(AugmentationSampleImage image) {
        return new InspectionImageCommentResponse(
                image.getAugmentationSampleImageId(),
                image.getComment(),
                image.getUpdatedAt()
        );
    }
}
