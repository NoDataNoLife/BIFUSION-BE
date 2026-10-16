package com.nodatanolife.bifusion_be.domain.job.service;

import com.nodatanolife.bifusion_be.domain.job.dto.response.AugmentedDatasetResponse;
import com.nodatanolife.bifusion_be.domain.job.dto.response.TrainedModelResponse;

import java.util.List;

public interface AiResultQueryService {

    /** 학습에 쓸 수 있는 증강 결과 목록 (학습 요청 검증과 같은 기준) */
    List<AugmentedDatasetResponse> getAugmentedDatasets(Long userId, Long projectId);

    /** 추론에 쓸 수 있는 학습 모델 목록 (추론 요청 검증과 같은 기준) */
    List<TrainedModelResponse> getTrainedModels(Long userId, Long projectId);
}
