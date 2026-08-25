package com.nodatanolife.bifusion_be.domain.notification.service;

import com.nodatanolife.bifusion_be.domain.notification.dto.response.NotificationResponse;
import com.nodatanolife.bifusion_be.domain.notification.entity.NotificationCategory;
import com.nodatanolife.bifusion_be.domain.notification.entity.NotificationType;
import com.nodatanolife.bifusion_be.domain.notification.entity.TargetType;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    void create(User user, NotificationType type, String title, String content,
                TargetType targetType, Long targetId);

    void deleteByUser(User user);

    Page<NotificationResponse> getNotifications(
            Long userId, NotificationCategory category, Boolean isRead, String keyword, Pageable pageable);

    void softDeleteNotifications(Long userId, Long notificationId);
}