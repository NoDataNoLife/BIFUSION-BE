package com.nodatanolife.bifusion_be.domain.job.ml.dto.response;

/**
 * ML 결과 파일 묶음 (result.artifacts). 모든 필드 nullable.
 *
 * @param download 다운로드용 파일 (증강: 데이터셋, 학습: 모델)
 * @param preview  미리보기 이미지 (증강만)
 */
public record MlArtifacts(
        MlArtifact download,
        MlArtifact preview
) {
}
