package com.nodatanolife.bifusion_be.domain.dataset.dto.response;

import com.nodatanolife.bifusion_be.domain.dataset.entity.Dataset;
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
        String fileName,
        @PresignedUrl String downloadUrl
) {
    public record AuthorInfo(Long userId, String nickname, @PresignedUrl String profileImageUrl) {}

    public static DatasetDetailResponse from(Dataset dataset, List<String> tags) {
        File file = dataset.getFile();

        return new DatasetDetailResponse(
                dataset.getDatasetId(),
                dataset.getTitle(),
                dataset.getDescription(),
                dataset.getIsExpertVerified(),
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
                file != null ? file.getFileName() : null,
                file != null ? file.getS3Key() : null
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