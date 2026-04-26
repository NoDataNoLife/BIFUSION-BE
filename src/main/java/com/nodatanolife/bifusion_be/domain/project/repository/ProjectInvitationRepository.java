package com.nodatanolife.bifusion_be.domain.project.repository;

import com.nodatanolife.bifusion_be.domain.project.entity.ProjectInvitation;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ProjectInvitationRepository extends JpaRepository<ProjectInvitation, Long> {

    void deleteByInviterOrInvitee(User inviter, User invitee);

    Optional<ProjectInvitation> findFirstByProjectProjectIdAndInviteeEmailOrderByLastSentAtDesc(
            Long projectId,
            String inviteeEmail
    );

    @Modifying
    @Query("""
    UPDATE ProjectInvitation p
    SET p.status = 'EXPIRED'
    WHERE p.status = 'PENDING'
    AND p.createdAt < :expiredBefore
    """)
    int bulkExpire(@Param("expiredBefore") LocalDateTime expiredBefore);
}
