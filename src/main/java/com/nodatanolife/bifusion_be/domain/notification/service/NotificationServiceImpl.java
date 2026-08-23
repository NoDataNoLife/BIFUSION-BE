package com.nodatanolife.bifusion_be.domain.notification.service;

import com.nodatanolife.bifusion_be.domain.notification.entity.Notification;
import com.nodatanolife.bifusion_be.domain.notification.entity.NotificationType;
import com.nodatanolife.bifusion_be.domain.notification.entity.TargetType;
import com.nodatanolife.bifusion_be.domain.notification.repository.NotificationRepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

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
}