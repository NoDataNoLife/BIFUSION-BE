package com.nodatanolife.bifusion_be.domain.job.service;

/**
 * 트랜잭션 밖에서 미리 조회한 ML 결과 파일 정보. 크기·형식은 S3 조회에 실패하면 null.
 */
public record ResultFileMeta(
        String s3Key,
        Long size,
        String contentType
) {
}
