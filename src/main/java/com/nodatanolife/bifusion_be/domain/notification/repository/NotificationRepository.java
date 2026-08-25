package com.nodatanolife.bifusion_be.domain.notification.repository;

import com.nodatanolife.bifusion_be.domain.notification.entity.Notification;
import com.nodatanolife.bifusion_be.domain.notification.entity.NotificationType;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Modifying;
import java.util.Optional;
import java.util.List;


public interface NotificationRepository extends JpaRepository<Notification, Long> {

    void deleteByUser(User user);

    @Query("""
        SELECT n FROM Notification n
        WHERE n.user = :user
          AND n.isDeleted = false
          AND (:types IS NULL OR n.type IN :types)
          AND (:isRead IS NULL OR n.isRead = :isRead)
          AND (:keyword IS NULL
               OR LOWER(n.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
               OR LOWER(n.content) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
        ORDER BY n.createdAt DESC
        """)
    Page<Notification> findByFilters(
            @Param("user") User user,
            @Param("types") List<NotificationType> types,
            @Param("isRead") Boolean isRead,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    long countByUserAndIsReadFalse(User user);

    Optional<Notification> findByNotificationIdAndUserAndIsDeletedFalse(Long notificationId, User user);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isDeleted = true WHERE n.user = :user AND n.isDeleted = false")
    void softDeleteAllByUser(@Param("user") User user);


}
