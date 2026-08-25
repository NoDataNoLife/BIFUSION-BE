package com.nodatanolife.bifusion_be.domain.user.service;

import com.nodatanolife.bifusion_be.domain.user.dto.response.MyActivityListResponse;

public interface MyActivityService {

    MyActivityListResponse getMyActivities(Long userId, String cursor, int size);
}
