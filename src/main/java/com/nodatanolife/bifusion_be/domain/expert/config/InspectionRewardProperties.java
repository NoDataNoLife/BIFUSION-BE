package com.nodatanolife.bifusion_be.domain.expert.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 전문가 검수 신청 리워드(rewardPoints) 허용 범위 (inspection.reward). 검수 대상의 스토리지 크기(MB, 올림)에 비례한다.
 *
 * @param minPoints      최소 리워드의 하한
 * @param baseMaxPoints  최대 리워드의 하한 (크기가 작거나 알 수 없는 대상, 레시피)
 * @param maxPoints      최대 리워드의 상한 (대상과 무관)
 * @param minPointsPerMb 1MB당 최소 리워드
 * @param maxPointsPerMb 1MB당 최대 리워드
 */
@ConfigurationProperties(prefix = "inspection.reward")
public record InspectionRewardProperties(
        int minPoints,
        int baseMaxPoints,
        int maxPoints,
        int minPointsPerMb,
        int maxPointsPerMb
) {
}
