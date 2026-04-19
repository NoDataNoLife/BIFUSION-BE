package com.nodatanolife.bifusion_be.domain.activityLog.service;

import com.nodatanolife.bifusion_be.domain.user.entity.User;

public interface ActivityLogService {

    void anonymizeByUser(User user, User deletedUser);
}
