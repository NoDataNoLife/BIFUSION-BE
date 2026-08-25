package com.nodatanolife.bifusion_be.domain.dataset.dto.response;

import com.nodatanolife.bifusion_be.domain.dataset.entity.Dataset;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionSummary;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionStatus;
import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;

import java.time.LocalDate;
import java.util.List;

/**
 * "내 자산" 데이터셋 목록 조회 응답 DTO.
 * DatasetRepository#findAllByTypeAndUser_UserId가 공개 여부와 무관하게
 * 본인 소유 데이터셋을 전부 조회하므로, 공개/비공개 데이터셋이 함께 담길 수 있다.
 */
public record MyDatasetListResponse(
        Long datasetId,
        String name,
        Boolean expertVerified,
        @PresignedUrl String bannerImageUrl,
        Long totalSize,
        Long fileCount,
        LocalDate uploadedAt,
        List<String> fileFormats
) {
    public static MyDatasetListResponse from(Dataset dataset, InspectionSummary verificationSummary) {
        return new MyDatasetListResponse(
                dataset.getDatasetId(),
                dataset.getTitle(),
                verificationSummary.status() == InspectionStatus.COMPLETED,
                dataset.getBannerImageUrl(),
                dataset.getFile() != null ? dataset.getFile().getFileSize() : null,
                dataset.getFileCount() != null ? dataset.getFileCount().longValue() : 0L,
                dataset.getCreatedAt().toLocalDate(),
                dataset.getFormat() != null ? List.of(dataset.getFormat()) : List.of()
        );
    }
}
