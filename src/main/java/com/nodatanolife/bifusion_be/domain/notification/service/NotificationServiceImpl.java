package com.nodatanolife.bifusion_be.domain.notification.service;

import com.nodatanolife.bifusion_be.domain.notification.entity.Notification;
import com.nodatanolife.bifusion_be.domain.notification.entity.NotificationType;
import com.nodatanolife.bifusion_be.domain.notification.entity.TargetType;
import com.nodatanolife.bifusion_be.domain.notification.repository.NotificationRepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * 알림 생성
     */
    @Override
    public void create(User user, NotificationType type, String title, String content,
                       TargetType targetType, Long targetId) {
        Notification notification = Notification.create(user, type, title, content, targetType, targetId);
        notificationRepository.save(notification);
    }

    /**
     * 회원 탈퇴 시 알림 삭제
     */
    @Override
    public void deleteByUser(User user) {
        notificationRepository.deleteByUser(user);
    }

    @Override
    public void markAsRead(Long userId, Long notificationId) {
        User user = userRepository.getReferenceById(userId);
        Notification notification = notificationRepository.findByNotificationIdAndUser(notificationId, user)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        notification.markAsRead();
    }

    @Override
    public void markAllAsRead(Long userId) {
        User user = userRepository.getReferenceById(userId);
        notificationRepository.markAllAsRead(user);
    }
}