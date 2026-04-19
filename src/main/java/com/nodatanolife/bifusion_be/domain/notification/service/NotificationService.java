package com.nodatanolife.bifusion_be.domain.notification.service;

import com.nodatanolife.bifusion_be.domain.user.entity.User;

public interface NotificationService {

    void deleteByUser(User user);
}
