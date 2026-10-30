package com.nodatanolife.bifusion_be.domain.dataset.service;

/**
 * 데이터셋 첨부 파일에서 뽑은 정보.
 *
 * @param bannerKey 대표 이미지의 S3 key. 뽑을 수 없으면 null
 * @param fileCount 데이터셋에 든 파일 수. zip이면 안에 든 파일 수, 그 외는 1. 셀 수 없으면 null
 */
public record DatasetFileSummary(String bannerKey, Integer fileCount) {
}
