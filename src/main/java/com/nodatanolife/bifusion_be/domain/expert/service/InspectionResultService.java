package com.nodatanolife.bifusion_be.domain.expert.service;

import com.nodatanolife.bifusion_be.domain.dataset.entity.Dataset;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionSummary;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionTargetResponse;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationConfig;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;
import com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe;

import java.util.List;
import java.util.Map;

/**
 * Recipe/Dataset가 공통으로 사용하는 검증 상태/결과 조회 헬퍼.
 * targetType + targetId 기준으로 가장 최근 검수 신청(Inspection) 건을 찾아 상태와 결과를 파생시킨다.
 */
public interface InspectionResultService {

    InspectionSummary getSummary(InspectionTargetType targetType, Long targetId);

    Map<Long, InspectionSummary> getSummaries(InspectionTargetType targetType, List<Long> targetIds);

    /**
     * 증강 결과별 검수 신청 대상 정보. canRequest는 검수 신청 API의 소유자·중복 검증과 같은 기준이다.
     *
     * @param userId 조회하는 사용자
     * @return AugmentationConfig ID → 검수 신청 대상 정보
     */
    Map<Long, InspectionTargetResponse> getAugmentedDataTargets(Long userId, List<AugmentationConfig> configs);

    /**
     * @return Recipe ID → 검수 신청 대상 정보
     */
    Map<Long, InspectionTargetResponse> getRecipeTargets(Long userId, List<Recipe> recipes);

    /**
     * 업로드(UPLOADED) 데이터셋만 담는다. 증강 데이터셋은 결과에 없다.
     *
     * @return Dataset ID → 검수 신청 대상 정보
     */
    Map<Long, InspectionTargetResponse> getUploadedDataTargets(Long userId, List<Dataset> datasets);
}
