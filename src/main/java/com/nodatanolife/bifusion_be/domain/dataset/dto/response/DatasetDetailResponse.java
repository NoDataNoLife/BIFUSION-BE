package com.nodatanolife.bifusion_be.domain.dataset.dto.response;

import com.nodatanolife.bifusion_be.domain.dataset.entity.Dataset;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionResultResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionSummary;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionTargetResponse;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionStatus;
import com.nodatanolife.bifusion_be.domain.file.entity.File;
import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 데이터셋 상세 조회 응답 DTO.
 * 비공개 데이터셋인 경우 서비스단(DatasetServiceImpl#getDatasetDetail)에서
 * 작성자 본인이 아니면 이 응답을 만들기 전에 이미 예외를 던지므로,
 * 여기까지 도달했다면 "공개 데이터셋" 또는 "본인 소유 비공개 데이터셋"만 담긴다.
 */
public record DatasetDetailResponse(
        Long datasetId,
        String title,
        String description,
        // 첨부 파일에서 뽑은 대표 이미지 (DatasetBannerExtractor). 뽑지 못했으면 null
        @PresignedUrl String bannerImageUrl,
        boolean isExpertVerified,
        List<String> tags,
        String fileSize,
        Integer fileCount,
        Integer downloadCount,
        AuthorInfo author,
        String category,
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
        InspectionStatus inspectionStatus,
        InspectionResultResponse inspectionResult,
        // 검수 신청 버튼·취소용. 작성자 본인의 업로드 데이터셋에만 내려주고 그 외에는 null
        InspectionTargetResponse inspectionTarget
) {
    /**
     * 작성자 정보. profileImageUrl은 @PresignedUrl로 직렬화 시점에 presign된다 (S3 버킷 private).
     */
    public record AuthorInfo(Long userId, String nickname, @PresignedUrl String profileImageUrl) {}

    public static DatasetDetailResponse from(Dataset dataset, List<String> tags, InspectionSummary verificationSummary,
                                             InspectionTargetResponse inspectionTarget) {
        File file = dataset.getFile();

        return new DatasetDetailResponse(
                dataset.getDatasetId(),
                dataset.getTitle(),
                dataset.getDescription(),
                dataset.getBannerImageUrl(),
                verificationSummary.status() == InspectionStatus.COMPLETED,
                tags,
                formatFileSize(file != null ? file.getFileSize() : null),
                dataset.getFileCount(),
                dataset.getDownloadCount(),
                new AuthorInfo(
                        dataset.getUser().getUserId(),
                        dataset.getUser().getNickname(),
                        dataset.getUser().getProfileImageUrl()
                ),
                dataset.getCategory(),
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
                verificationSummary.result(),
                inspectionTarget
        );
    }

    /**
     * 바이트 단위 파일 크기를 사람이 읽기 쉬운 단위(GB/MB/KB/B)로 변환한다.
     */
    private static String formatFileSize(Long bytes) {
        if (bytes == null) return null;
        if (bytes >= 1024L * 1024 * 1024) return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        if (bytes >= 1024L * 1024)        return String.format("%.1f MB", bytes / (1024.0 * 1024));
        if (bytes >= 1024L)               return String.format("%.1f KB", bytes / 1024.0);
        return bytes + " B";
    }
}