package com.nodatanolife.bifusion_be.domain.dataset.dto.response;

import com.nodatanolife.bifusion_be.domain.dataset.entity.Dataset;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 데이터셋 수정 응답 DTO.
 */
public record DatasetUpdateResponse(
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
        List<String> tags,
        Long fileId,
        String fileName,
        LocalDateTime updatedAt
) {
    public static DatasetUpdateResponse from(Dataset dataset, List<String> tags) {
        return new DatasetUpdateResponse(
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
                tags,
                dataset.getFile() != null ? dataset.getFile().getFileId() : null,
                dataset.getFile() != null ? dataset.getFile().getFileName() : null,
                dataset.getUpdatedAt()
        );
    }
}
