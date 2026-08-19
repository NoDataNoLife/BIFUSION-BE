package com.nodatanolife.bifusion_be.domain.dataset.dto.response;

import com.nodatanolife.bifusion_be.domain.dataset.entity.Dataset;

import java.time.LocalDateTime;

/**
 * 데이터셋 생성 응답 DTO.
 * isPublic을 함께 내려주어 프론트가 어떤 공개 범위로 저장됐는지 즉시 확인할 수 있도록 한다.
 */
public record DatasetCreateResponse(
        Long datasetId,
        String title,
        String category,
        String license,
        Boolean isPublic,
        LocalDateTime createdAt
) {
    public static DatasetCreateResponse from(Dataset dataset) {
        return new DatasetCreateResponse(
                dataset.getDatasetId(),
                dataset.getTitle(),
                dataset.getCategory(),
                dataset.getLicense(),
                dataset.getIsPublic(),
                dataset.getCreatedAt()
        );
    }
}