package com.nodatanolife.bifusion_be.domain.notification.dto.response;

import com.nodatanolife.bifusion_be.domain.notification.entity.Notification;
import com.nodatanolife.bifusion_be.domain.notification.entity.NotificationType;
import com.nodatanolife.bifusion_be.domain.notification.entity.TargetType;
import com.nodatanolife.bifusion_be.domain.project.entity.InvitationStatus;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectInvitation;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectRole;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long notificationId,
        NotificationType type,
        String title,
        String content,
        TargetType targetType,
        Long targetId,
        Boolean isRead,
        LocalDateTime createdAt,
        // 프로젝트 초대 알림(PROJECT_INVITATION)에만 채운다. 알림창에서 바로 수락·거절할 때 쓴다
        InvitationInfo invitation
) {
    /**
     * 조회 시점의 초대 상태. PENDING일 때만 PATCH /projects/{projectId}/invitations/{invitationId}로 응답할 수 있다.
     */
    public record InvitationInfo(Long invitationId, Long projectId, InvitationStatus status, ProjectRole role) {
        public static InvitationInfo from(ProjectInvitation invitation) {
            return new InvitationInfo(
                    invitation.getProjectInvitationId(),
                    invitation.getProject().getProjectId(),
                    invitation.getStatus(),
                    invitation.getRole()
            );
        }
    }

    /**
     * @param invitation 초대 알림이 가리키는 초대. 초대 알림이 아니거나 초대가 지워졌으면 null
     */
    public static NotificationResponse from(Notification notification, ProjectInvitation invitation) {
        return new NotificationResponse(
                notification.getNotificationId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getTargetType(),
                notification.getTargetId(),
                notification.getIsRead(),
                notification.getCreatedAt(),
                invitation != null ? InvitationInfo.from(invitation) : null
        );
    }
}