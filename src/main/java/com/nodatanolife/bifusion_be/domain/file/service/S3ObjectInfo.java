package com.nodatanolife.bifusion_be.domain.file.service;

/**
 * S3 HeadObject 결과.
 */
public record S3ObjectInfo(
        Long size,
        String contentType
) {
}
