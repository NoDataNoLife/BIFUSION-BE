package com.nodatanolife.bifusion_be.domain.expert.dto.response;

import java.util.List;

public record ExpertTaskGroupedResponse(
        List<ExpertTaskSummaryResponse> pending,
        List<ExpertTaskSummaryResponse> inProgress,
        List<ExpertTaskSummaryResponse> completed
) {
}
