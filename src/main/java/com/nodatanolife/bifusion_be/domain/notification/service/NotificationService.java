package com.nodatanolife.bifusion_be.domain.notification.service;

import com.nodatanolife.bifusion_be.domain.notification.entity.NotificationType;
import com.nodatanolife.bifusion_be.domain.notification.entity.TargetType;
import com.nodatanolife.bifusion_be.domain.user.entity.User;

public interface NotificationService {

    void create(User user, NotificationType type, String title, String content,
                TargetType targetType, Long targetId);

    void deleteByUser(User user);
}