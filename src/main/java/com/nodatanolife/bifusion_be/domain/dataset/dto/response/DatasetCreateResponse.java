package com.nodatanolife.bifusion_be.domain.dataset.dto.response;

import com.nodatanolife.bifusion_be.domain.dataset.entity.Dataset;

import java.time.LocalDateTime;

public record DatasetCreateResponse(
        Long datasetId,
        String title,
        String category,
        String license,
        LocalDateTime createdAt
) {
    public static DatasetCreateResponse from(Dataset dataset) {
        return new DatasetCreateResponse(
                dataset.getDatasetId(),
                dataset.getTitle(),
                dataset.getCategory(),
                dataset.getLicense(),
                dataset.getCreatedAt()
        );
    }
}