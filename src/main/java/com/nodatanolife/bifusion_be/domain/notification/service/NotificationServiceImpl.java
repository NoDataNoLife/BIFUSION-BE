package com.nodatanolife.bifusion_be.domain.notification.service;

import com.nodatanolife.bifusion_be.domain.notification.dto.response.NotificationResponse;
import com.nodatanolife.bifusion_be.domain.notification.entity.NotificationCategory;
import com.nodatanolife.bifusion_be.domain.notification.entity.Notification;
import com.nodatanolife.bifusion_be.domain.notification.entity.NotificationType;
import com.nodatanolife.bifusion_be.domain.notification.entity.TargetType;
import com.nodatanolife.bifusion_be.domain.notification.repository.NotificationRepository;
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;

import java.util.List;


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
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(
            Long userId, NotificationCategory category, Boolean isRead, String keyword, Pageable pageable) {

        User user = userRepository.getReferenceById(userId);
        List<NotificationType> types = category == null ? null : category.getTypes();

        return notificationRepository.findByFilters(user, types, isRead, keyword, pageable)
                .map(NotificationResponse::from);
    }

    @Override
    public void hardDeleteNotifications(Long userId, Long notificationId) {
        User user = userRepository.getReferenceById(userId);

        if (notificationId != null) {
            Notification notification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

            if (!notification.getUser().getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN_NOTIFICATION_ACCESS);
            }

            notificationRepository.delete(notification);
        } else {
            notificationRepository.deleteByUser(user);
        }
    }

    @Override
    public void softDeleteNotifications(Long userId, Long notificationId) {
        User user = userRepository.getReferenceById(userId);

        if (notificationId != null) {
            Notification notification = notificationRepository
                    .findByNotificationIdAndUserAndIsDeletedFalse(notificationId, user)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
            notification.delete();
        } else {
            notificationRepository.softDeleteAllByUser(user);
        }
    }
}