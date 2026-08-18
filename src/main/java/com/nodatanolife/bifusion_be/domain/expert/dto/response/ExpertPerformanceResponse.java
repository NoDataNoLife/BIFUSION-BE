package com.nodatanolife.bifusion_be.domain.expert.dto.response;

import java.util.Map;

public record ExpertPerformanceResponse(
        boolean isExpertCertified,
        int totalReward,
        Integer topPercent,
        long completedCount,
        Double feedbackSatisfaction,
        Map<String, Long> taskStats
) {
}
