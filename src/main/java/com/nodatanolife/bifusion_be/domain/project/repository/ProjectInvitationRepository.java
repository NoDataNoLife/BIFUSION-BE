package com.nodatanolife.bifusion_be.domain.project.repository;

import com.nodatanolife.bifusion_be.domain.project.entity.InvitationStatus;
import com.nodatanolife.bifusion_be.domain.project.entity.Project;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectInvitation;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProjectInvitationRepository extends JpaRepository<ProjectInvitation, Long> {

    void deleteByInviterOrInvitee(User inviter, User invitee);

    void deleteByProject(Project project);

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

    List<ProjectInvitation> findAllByInviteeEmailAndStatus(String email, InvitationStatus status);

    // 초대 알림 응답용. targetType=INVITATION 알림의 초대를 한 번에 조회한다
    @Query("SELECT p FROM ProjectInvitation p JOIN FETCH p.project WHERE p.projectInvitationId IN :ids")
    List<ProjectInvitation> findAllWithProjectByIdIn(@Param("ids") List<Long> ids);

    // targetType=PROJECT로 저장된 예전 초대 알림용. 프로젝트별 최신 초대는 호출 측에서 고른다
    @Query("SELECT p FROM ProjectInvitation p JOIN FETCH p.project WHERE p.invitee = :invitee AND p.project.projectId IN :projectIds")
    List<ProjectInvitation> findAllWithProjectByInviteeAndProjectIdIn(@Param("invitee") User invitee,
                                                                      @Param("projectIds") List<Long> projectIds);
}
