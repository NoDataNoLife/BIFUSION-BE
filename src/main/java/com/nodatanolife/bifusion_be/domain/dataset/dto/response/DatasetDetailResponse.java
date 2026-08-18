package com.nodatanolife.bifusion_be.domain.dataset.dto.response;

import com.nodatanolife.bifusion_be.domain.dataset.entity.Dataset;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.VerificationResultResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.VerificationSummary;
import com.nodatanolife.bifusion_be.domain.expert.entity.VerificationStatus;
import com.nodatanolife.bifusion_be.domain.file.entity.File;
import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;

import java.time.LocalDateTime;
import java.util.List;

public record DatasetDetailResponse(
        Long datasetId,
        String title,
        String description,
        boolean isExpertVerified,
        List<String> tags,
        String fileSize,
        Integer fileCount,
        Integer downloadCount,
        AuthorInfo author,
        String license,
        LocalDateTime createdAt,
        String format,
        String imageType,
        String resolution,
        String classes,
        String usageExample,
        // presigned URL을 여기서 바로 내려주지 않음 - 다운로드 클릭 시 POST /api/v1/files/{fileId}/download로 온디맨드 발급받아야 함
        Long fileId,
        String fileName,
        VerificationStatus verificationStatus,
        VerificationResultResponse verificationResult
) {
    public record AuthorInfo(Long userId, String nickname, @PresignedUrl String profileImageUrl) {}

    public static DatasetDetailResponse from(Dataset dataset, List<String> tags, VerificationSummary verificationSummary) {
        File file = dataset.getFile();

        return new DatasetDetailResponse(
                dataset.getDatasetId(),
                dataset.getTitle(),
                dataset.getDescription(),
                verificationSummary.status() == VerificationStatus.COMPLETED,
                tags,
                formatFileSize(file != null ? file.getFileSize() : null),
                dataset.getFileCount(),
                dataset.getDownloadCount(),
                new AuthorInfo(
                        dataset.getUser().getUserId(),
                        dataset.getUser().getNickname(),
                        dataset.getUser().getProfileImageUrl()
                ),
                dataset.getLicense(),
                dataset.getCreatedAt(),
                dataset.getFormat(),
                dataset.getImageType(),
                dataset.getResolution(),
                dataset.getClasses(),
                dataset.getUsageExample(),
                file != null ? file.getFileId() : null,
                file != null ? file.getFileName() : null,
                verificationSummary.status(),
                verificationSummary.result()
        );
    }

    private static String formatFileSize(Long bytes) {
        if (bytes == null) return null;
        if (bytes >= 1024L * 1024 * 1024) return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        if (bytes >= 1024L * 1024)        return String.format("%.1f MB", bytes / (1024.0 * 1024));
        if (bytes >= 1024L)               return String.format("%.1f KB", bytes / 1024.0);
        return bytes + " B";
    }
}