package com.nodatanolife.bifusion_be.domain.dataset.dto.response;

import com.nodatanolife.bifusion_be.domain.dataset.entity.Dataset;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.VerificationSummary;
import com.nodatanolife.bifusion_be.domain.expert.entity.VerificationStatus;
import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;

import java.time.LocalDate;
import java.util.List;

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
    public static MyDatasetListResponse from(Dataset dataset, VerificationSummary verificationSummary) {
        return new MyDatasetListResponse(
                dataset.getDatasetId(),
                dataset.getTitle(),
                verificationSummary.status() == VerificationStatus.COMPLETED,
                dataset.getBannerImageUrl(),
                dataset.getFile() != null ? dataset.getFile().getFileSize() : null,
                dataset.getFileCount().longValue(),
                dataset.getCreatedAt().toLocalDate(),
                dataset.getFormat() != null ? List.of(dataset.getFormat()) : List.of()
        );
    }
}
