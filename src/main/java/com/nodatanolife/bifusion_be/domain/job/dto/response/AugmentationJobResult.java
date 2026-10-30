package com.nodatanolife.bifusion_be.domain.job.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationConfig;

/**
 * @param datasetId 학습 요청의 dataset_id로 쓰는 값 (= 이 증강 jobId)
 */
@Schema(description = "증강 결과")
public record AugmentationJobResult(
        @Schema(description = "학습 요청의 dataset_id로 쓰는 값 (= 이 증강 작업 번호)", example = "15")
        Long datasetId,
        @Schema(description = "새로 만든 이미지 수", example = "20")
        Integer generatedCount,
        @Schema(description = "전체 이미지 수 (원본 + 생성분)", example = "30")
        Integer totalSampleCount,
        @Schema(description = "미리보기 이미지 주소 (presigned, 없으면 null)")
        @PresignedUrl String previewUrl,
        @Schema(description = "결과 데이터 다운로드 주소 (presigned, 없으면 null). 결과 npz와 PNG 이미지를 묶은 zip이며, zip이 없는 결과는 npz")
        @PresignedUrl String downloadUrl
) implements JobResult {

    public static AugmentationJobResult from(AugmentationConfig config) {
        return new AugmentationJobResult(
                config.getJob().getJobId(),
                config.getGeneratedCount(),
                config.getTotalSampleCount(),
                config.getPreviewKey(),
                config.getDownloadKey()
        );
    }
}
