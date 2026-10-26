package com.nodatanolife.bifusion_be.domain.notification.service;

import com.nodatanolife.bifusion_be.domain.notification.dto.response.NotificationResponse;
import com.nodatanolife.bifusion_be.domain.notification.entity.NotificationCategory;
import com.nodatanolife.bifusion_be.domain.notification.entity.Notification;
import com.nodatanolife.bifusion_be.domain.notification.entity.NotificationType;
import com.nodatanolife.bifusion_be.domain.notification.entity.TargetType;
import com.nodatanolife.bifusion_be.domain.notification.repository.NotificationRepository;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectInvitation;
import com.nodatanolife.bifusion_be.domain.project.repository.ProjectInvitationRepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ProjectInvitationRepository projectInvitationRepository;

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

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(
            Long userId, boolean excludePopupDeleted, NotificationCategory category, Boolean isRead, String keyword,
            Pageable pageable) {

        User user = userRepository.getReferenceById(userId);
        List<NotificationType> types = category == null ? null : category.getTypes();

        Page<Notification> page = notificationRepository.findByFilters(user, excludePopupDeleted, types, isRead, keyword, pageable);
        Map<Long, ProjectInvitation> invitationByNotificationId = findInvitations(user, page.getContent());
        return page.map(notification -> NotificationResponse.from(
                notification, invitationByNotificationId.get(notification.getNotificationId())));
    }

    /**
     * 초대 알림(PROJECT_INVITATION)이 가리키는 초대를 일괄 조회한다 (N+1 방지).
     * - targetType=INVITATION: targetId가 초대 ID
     * - targetType=PROJECT: INVITATION 도입 전 알림. targetId(프로젝트)에서 받은 가장 최근 초대로 맞춘다
     */
    private Map<Long, ProjectInvitation> findInvitations(User user, List<Notification> notifications) {
        List<Notification> invitationNotifications = notifications.stream()
                .filter(n -> n.getType() == NotificationType.PROJECT_INVITATION && n.getTargetId() != null)
                .toList();
        if (invitationNotifications.isEmpty()) {
            return Map.of();
        }

        List<Long> invitationIds = targetIdsOf(invitationNotifications, TargetType.INVITATION);
        Map<Long, ProjectInvitation> byId = invitationIds.isEmpty() ? Map.of()
                : projectInvitationRepository.findAllWithProjectByIdIn(invitationIds).stream()
                .collect(Collectors.toMap(ProjectInvitation::getProjectInvitationId, Function.identity()));

        List<Long> projectIds = targetIdsOf(invitationNotifications, TargetType.PROJECT);
        Map<Long, ProjectInvitation> latestByProjectId = projectIds.isEmpty() ? Map.of()
                : projectInvitationRepository.findAllWithProjectByInviteeAndProjectIdIn(user, projectIds).stream()
                .collect(Collectors.toMap(i -> i.getProject().getProjectId(), Function.identity(),
                        BinaryOperator.maxBy(Comparator.comparing(ProjectInvitation::getLastSentAt,
                                Comparator.nullsFirst(Comparator.naturalOrder())))));

        Map<Long, ProjectInvitation> result = new HashMap<>();
        for (Notification n : invitationNotifications) {
            ProjectInvitation invitation = n.getTargetType() == TargetType.INVITATION
                    ? byId.get(n.getTargetId())
                    : latestByProjectId.get(n.getTargetId());
            if (invitation != null) {
                result.put(n.getNotificationId(), invitation);
            }
        }
        return result;
    }

    private static List<Long> targetIdsOf(List<Notification> notifications, TargetType targetType) {
        return notifications.stream()
                .filter(n -> n.getTargetType() == targetType)
                .map(Notification::getTargetId)
                .distinct()
                .toList();
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
            notification.softDelete();
        } else {
            notificationRepository.softDeleteAllByUser(user);
        }
    }
}