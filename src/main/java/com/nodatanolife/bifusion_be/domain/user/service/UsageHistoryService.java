package com.nodatanolife.bifusion_be.domain.user.service;

import com.nodatanolife.bifusion_be.domain.user.entity.User;

public interface UsageHistoryService {

    void deleteByUser(User user);
}
