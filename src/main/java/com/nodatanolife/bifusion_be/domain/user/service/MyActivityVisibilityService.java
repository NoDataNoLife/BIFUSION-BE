package com.nodatanolife.bifusion_be.domain.user.service;

import com.nodatanolife.bifusion_be.domain.user.dto.request.ActivityVisibilityUpdateRequest;
import com.nodatanolife.bifusion_be.domain.user.dto.response.ActivityVisibilityUpdateResponse;

public interface MyActivityVisibilityService {

    ActivityVisibilityUpdateResponse updateVisibilities(Long userId, ActivityVisibilityUpdateRequest request);
}
