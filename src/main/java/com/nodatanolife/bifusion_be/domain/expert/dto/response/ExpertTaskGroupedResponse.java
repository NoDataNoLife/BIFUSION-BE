package com.nodatanolife.bifusion_be.domain.expert.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ExpertTaskGroupedResponse(
        @JsonProperty("PENDING") List<ExpertTaskSummaryResponse> pending,
        @JsonProperty("IN_PROGRESS") List<ExpertTaskSummaryResponse> inProgress,
        @JsonProperty("COMPLETED") List<ExpertTaskSummaryResponse> completed
) {
}
