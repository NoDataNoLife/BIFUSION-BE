package com.nodatanolife.bifusion_be.domain.project.repository;

import com.nodatanolife.bifusion_be.domain.project.entity.ProjectInvitation;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectInvitationRepository extends JpaRepository<ProjectInvitation, Long> {

    void deleteByInviterOrInvitee(User inviter, User invitee);

    Optional<ProjectInvitation> findFirstByProjectProjectIdAndInviteeEmailOrderByLastSentAtDesc(
            Long projectId,
            String inviteeEmail
    );
}
