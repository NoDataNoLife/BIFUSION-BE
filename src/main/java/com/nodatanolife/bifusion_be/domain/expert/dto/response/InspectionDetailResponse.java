package com.nodatanolife.bifusion_be.domain.expert.dto.response;

import com.nodatanolife.bifusion_be.domain.dataset.entity.Dataset;
import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertTask;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequest;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequestStatus;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;
import com.nodatanolife.bifusion_be.domain.file.entity.File;
import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationConfig;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationSampleImage;
import com.nodatanolife.bifusion_be.domain.job.entity.SampleImageLabel;
import com.nodatanolife.bifusion_be.domain.recipe.dto.response.RecipeDetailResponse;
import com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe;
import com.nodatanolife.bifusion_be.domain.user.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 검수 중인 job(전문가 검수 요청) 상세 조회 응답 DTO.
 * inspectionId는 ExpertTask.expertTaskId를 가리킨다 (검수 시작 API와 동일한 리소스 기준).
 * targetType에 해당하는 영역만 채우고 나머지는 null이다.
 * - AUGMENTED_DATA: jobTitle, augmentationParams, sampleImages
 * - RECIPE: recipe
 * - UPLOADED_DATA: dataset
 */
public record InspectionDetailResponse(
        Long inspectionId,
        String reviewCode,
        InspectionTargetType targetType,
        String jobTitle,
        InspectionRequestStatus status,
        Requester requester,
        String reason,
        AugmentationParams augmentationParams,
        SampleImages sampleImages,
        RecipeTarget recipe,
        DatasetTarget dataset,
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

    /**
     * 레시피 검수 대상. 개요·설정값은 레시피 상세 응답과 같은 구조를 쓴다.
     * 완료된 검수 건은 레시피가 삭제된 뒤에도 조회되므로 isDeleted를 함께 내려준다.
     */
    public record RecipeTarget(
            Long recipeId,
            String title,
            String description,
            @PresignedUrl String bannerUrl,
            RecipeDetailResponse.Overview overview,
            RecipeDetailResponse.Settings settings,
            boolean isDeleted
    ) {
        public static RecipeTarget from(Recipe recipe) {
            return new RecipeTarget(
                    recipe.getRecipeId(),
                    recipe.getTitle(),
                    recipe.getShortDescription(),
                    recipe.getThumbnailUrl(),
                    RecipeDetailResponse.Overview.from(recipe),
                    RecipeDetailResponse.Settings.from(recipe),
                    recipe.isDeleted()
            );
        }
    }

    /**
     * 업로드 데이터셋 검수 대상. 파일은 데이터셋 상세와 같이 fileId로 POST /api/v1/files/{fileId}/download를 호출해 받는다.
     */
    public record DatasetTarget(
            Long datasetId,
            String title,
            String description,
            String category,
            String license,
            String format,
            String imageType,
            String resolution,
            String classes,
            String usageExample,
            Long fileId,
            String fileName,
            Long fileSize,
            boolean isDeleted
    ) {
        public static DatasetTarget from(Dataset dataset) {
            File file = dataset.getFile();
            return new DatasetTarget(
                    dataset.getDatasetId(),
                    dataset.getTitle(),
                    dataset.getDescription(),
                    dataset.getCategory(),
                    dataset.getLicense(),
                    dataset.getFormat(),
                    dataset.getImageType(),
                    dataset.getResolution(),
                    dataset.getClasses(),
                    dataset.getUsageExample(),
                    file != null ? file.getFileId() : null,
                    file != null ? file.getFileName() : null,
                    file != null ? file.getFileSize() : null,
                    dataset.isDeleted()
            );
        }
    }

    public static InspectionDetailResponse ofAugmentedData(ExpertTask task, AugmentationConfig config, List<AugmentationSampleImage> images) {
        return of(task, config.getJob().getProject().getTitle(),
                AugmentationParams.from(config), SampleImages.from(config, images), null, null);
    }

    public static InspectionDetailResponse ofRecipe(ExpertTask task, Recipe recipe) {
        return of(task, null, null, null, RecipeTarget.from(recipe), null);
    }

    public static InspectionDetailResponse ofUploadedData(ExpertTask task, Dataset dataset) {
        return of(task, null, null, null, null, DatasetTarget.from(dataset));
    }

    private static InspectionDetailResponse of(ExpertTask task, String jobTitle, AugmentationParams augmentationParams,
                                               SampleImages sampleImages, RecipeTarget recipe, DatasetTarget dataset) {
        InspectionRequest inspectionRequest = task.getInspectionRequest();

        return new InspectionDetailResponse(
                task.getExpertTaskId(),
                task.getReviewCode(),
                inspectionRequest.getTargetType(),
                jobTitle,
                inspectionRequest.getStatus(),
                Requester.from(inspectionRequest.getUser(), inspectionRequest.getAppliedAt()),
                inspectionRequest.getReason(),
                augmentationParams,
                sampleImages,
                recipe,
                dataset,
                task.getFeedback() != null ? task.getFeedback() : task.getDraftFeedback()
        );
    }
}
