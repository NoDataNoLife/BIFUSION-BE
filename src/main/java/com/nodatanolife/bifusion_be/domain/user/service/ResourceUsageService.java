package com.nodatanolife.bifusion_be.domain.user.service;

import com.nodatanolife.bifusion_be.domain.user.dto.response.ResourceUsageResponse;

public interface ResourceUsageService {

    ResourceUsageResponse getResourceUsage(Long userId);
}
