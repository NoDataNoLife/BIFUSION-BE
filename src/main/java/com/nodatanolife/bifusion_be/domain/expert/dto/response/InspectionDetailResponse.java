package com.nodatanolife.bifusion_be.domain.expert.dto.response;

import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertTask;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequest;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequestStatus;
import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationConfig;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationSampleImage;
import com.nodatanolife.bifusion_be.domain.job.entity.SampleImageLabel;
import com.nodatanolife.bifusion_be.domain.user.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 검수 중인 job(전문가 검수 요청) 상세 조회 응답 DTO.
 * inspectionId는 ExpertTask.expertTaskId를 가리킨다 (검수 시작 API와 동일한 리소스 기준).
 */
public record InspectionDetailResponse(
        Long inspectionId,
        String reviewCode,
        String jobTitle,
        InspectionRequestStatus status,
        Requester requester,
        AugmentationParams augmentationParams,
        SampleImages sampleImages,
        String expertComment
) {
    public record Requester(
            String name,
            String email,
            @PresignedUrl String profileImageUrl,
            LocalDateTime requestedAt
    ) {
        public static Requester from(User requester, LocalDateTime requestedAt) {
            return new Requester(
                    requester.getName(),
                    requester.getEmail(),
                    requester.getProfileImageUrl(),
                    requestedAt
            );
        }
    }

    public record AugmentationParams(
            Integer imagesPerClass,
            Integer sampling,
            BigDecimal guidance
    ) {
        public static AugmentationParams from(AugmentationConfig config) {
            return new AugmentationParams(
                    config.getImagesPerClass(),
                    config.getSamplingSteps(),
                    config.getGuidanceScale()
            );
        }
    }

    public record SampleImages(
            long generatedCount,
            long totalCount,
            List<SampleImage> images
    ) {
        public static SampleImages from(AugmentationConfig config, List<AugmentationSampleImage> images) {
            int normalCount = config.getNormalCount() == null ? 0 : config.getNormalCount();
            int anomalyCount = config.getAnomalyCount() == null ? 0 : config.getAnomalyCount();

            return new SampleImages(
                    images.size(),
                    (long) normalCount + anomalyCount,
                    images.stream().map(SampleImage::from).toList()
            );
        }
    }

    public record SampleImage(
            Long imageId,
            @PresignedUrl String imageUrl,
            String imageName,
            SampleImageLabel label,
            String comment
    ) {
        public static SampleImage from(AugmentationSampleImage image) {
            return new SampleImage(
                    image.getAugmentationSampleImageId(),
                    image.getImageUrl(),
                    image.getImageName(),
                    image.getLabel(),
                    image.getComment()
            );
        }
    }

    public static InspectionDetailResponse from(ExpertTask task, AugmentationConfig config, List<AugmentationSampleImage> images) {
        InspectionRequest inspectionRequest = task.getInspectionRequest();

        return new InspectionDetailResponse(
                task.getExpertTaskId(),
                task.getReviewCode(),
                config.getJob().getProject().getTitle(),
                inspectionRequest.getStatus(),
                Requester.from(inspectionRequest.getUser(), inspectionRequest.getAppliedAt()),
                AugmentationParams.from(config),
                SampleImages.from(config, images),
                task.getDraftFeedback()
        );
    }
}
