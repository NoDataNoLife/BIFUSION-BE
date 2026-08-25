package com.nodatanolife.bifusion_be.domain.notification.entity;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "title")
    private String title;

    @Column(name = "content")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 50)
    private TargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    @Column(name = "is_deleted", nullable = false)
    @org.hibernate.annotations.ColumnDefault("false")
    private Boolean isDeleted = false;

    @Builder(access = AccessLevel.PRIVATE)
    private Notification(User user, NotificationType type, String title, String content,
                         TargetType targetType, Long targetId) {
        this.user = user;
        this.type = type;
        this.title = title;
        this.content = content;
        this.targetType = targetType;
        this.targetId = targetId;
        this.isRead = false;
    }

    public static Notification create(User user, NotificationType type, String title, String content,
                                      TargetType targetType, Long targetId) {
        return Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .content(content)
                .targetType(targetType)
                .targetId(targetId)
                .build();
    }

    public void markAsRead() {
        this.isRead = true;
    }

    public void softDelete() {
        this.isDeleted = true;
    }

}
