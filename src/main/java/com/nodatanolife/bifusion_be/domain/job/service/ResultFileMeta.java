package com.nodatanolife.bifusion_be.domain.job.service;

/**
 * 트랜잭션 밖에서 미리 조회한 ML 결과 파일 정보.
 * 크기는 S3 조회 값(실제 저장된 크기)을 우선 쓰고, 조회에 실패하면 ML의 size_bytes를 쓴다. 형식은 S3 조회에 실패하면 null.
 */
public record ResultFileMeta(
        String s3Key,
        Long size,
        String contentType
) {

    public static String keyOf(ResultFileMeta meta) {
        return meta != null ? meta.s3Key() : null;
    }
}
