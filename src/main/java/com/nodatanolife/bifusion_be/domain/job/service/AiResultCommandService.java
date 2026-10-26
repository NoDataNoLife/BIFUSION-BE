package com.nodatanolife.bifusion_be.domain.job.service;

public interface AiResultCommandService {

    /** 증강 데이터 삭제 (결과 파일 S3·File 행 삭제, 목록·학습 선택지에서 제외) */
    void deleteAugmentedDataset(Long userId, Long projectId, Long datasetId);
}
