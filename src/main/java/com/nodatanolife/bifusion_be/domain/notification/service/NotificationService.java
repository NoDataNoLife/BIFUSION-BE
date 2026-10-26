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

    void markAsRead(Long userId, Long notificationId);
    void markAllAsRead(Long userId);

    /**
     * @param excludePopupDeleted true면 알림창에서 지운 알림을 뺀다 (알림창 목록). false면 전부 포함 (알림 전체목록)
     */
    Page<NotificationResponse> getNotifications(
            Long userId, boolean excludePopupDeleted, NotificationCategory category, Boolean isRead, String keyword,
            Pageable pageable);

    void hardDeleteNotifications(Long userId, Long notificationId);

    void softDeleteNotifications(Long userId, Long notificationId);
}