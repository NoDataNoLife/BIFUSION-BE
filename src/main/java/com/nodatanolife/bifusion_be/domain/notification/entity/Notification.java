package com.nodatanolife.bifusion_be.domain.notification.entity;

import com.nodatanolife.bifusion_be.domain.notification.entity.converter.NotificationTypeConverter;
import com.nodatanolife.bifusion_be.domain.notification.entity.converter.TargetTypeConverter;
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

    // 컨버터로 매핑: @Enumerated는 enum 값 목록 CHECK 제약을 만들고 ddl-auto: update로는 갱신되지 않아 새 타입 저장이 막힌다.
    // 기존 DB의 notification_type_check 제약은 직접 삭제해야 한다.
    @Convert(converter = NotificationTypeConverter.class)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "title")
    private String title;

    @Column(name = "content")
    private String content;

    // 컨버터로 매핑: @Enumerated는 enum 값 목록 CHECK 제약을 만들어 INVITATION 같은 값 추가 시 저장이 막힌다.
    // 기존 DB의 notification_target_type_check 제약은 직접 삭제해야 한다.
    @Convert(converter = TargetTypeConverter.class)
    @Column(name = "target_type", length = 50)
    private TargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    // 알림창(팝업)에서 지운 알림. 알림창 목록에서만 빠지고 알림 전체목록에는 그대로 남는다
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
