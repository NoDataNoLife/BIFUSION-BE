package com.nodatanolife.bifusion_be.domain.expert.service;

import com.nodatanolife.bifusion_be.domain.expert.dto.response.VerificationSummary;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;

import java.util.List;
import java.util.Map;

/**
 * Recipe/Dataset가 공통으로 사용하는 검증 상태/결과 조회 헬퍼.
 * targetType + targetId 기준으로 가장 최근 검수 신청(Inspection) 건을 찾아 상태와 결과를 파생시킨다.
 */
public interface VerificationResultService {

    VerificationSummary getSummary(InspectionTargetType targetType, Long targetId);

    Map<Long, VerificationSummary> getSummaries(InspectionTargetType targetType, List<Long> targetIds);
}
