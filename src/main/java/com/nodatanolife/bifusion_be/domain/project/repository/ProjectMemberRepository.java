package com.nodatanolife.bifusion_be.domain.project.repository;

import com.nodatanolife.bifusion_be.domain.project.entity.ProjectMember;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectRole;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    @Query("SELECT DISTINCT pm FROM ProjectMember pm " +
            "JOIN FETCH pm.project p " +
            "JOIN FETCH p.projectMembers m " +
            "JOIN FETCH m.user " +
            "WHERE pm.user.userId = :userId")

    List<ProjectMember> findAllByUserIdWithProjectAndMembers(@Param("userId") Long userId);

    long countByProjectProjectIdAndRole(Long projectId, ProjectRole role);

    void deleteByUser(User user);

    boolean existsByProjectProjectIdAndUserUserId(Long projectId, Long userId);

    Optional<ProjectMember> findByProjectProjectIdAndUserUserId(Long projectId, Long userId);

    @Query("""
            select count(pm) from ProjectMember pm
            where pm.user.userId = :userId
              and (:isPublic is null or pm.isPublic = :isPublic)
            """)
    long countMyProjects(
            @Param("userId") Long userId,
            @Param("isPublic") Boolean isPublic
    );

    @Query("""
            select pm from ProjectMember pm
            join fetch pm.project
            where pm.user.userId = :userId
              and (:isPublic is null or pm.isPublic = :isPublic)
            order by pm.joinedAt desc, pm.projectMemberId desc
            """)
    List<ProjectMember> findMyProjectsFirstPage(
            @Param("userId") Long userId,
            @Param("isPublic") Boolean isPublic,
            Pageable pageable
    );

    @Query("""
            select pm from ProjectMember pm
            join fetch pm.project
            where pm.user.userId = :userId
              and (:isPublic is null or pm.isPublic = :isPublic)
              and (
                pm.joinedAt < :cursorJoinedAt
                or (pm.joinedAt = :cursorJoinedAt and pm.projectMemberId < :cursorMemberId)
              )
            order by pm.joinedAt desc, pm.projectMemberId desc
            """)
    List<ProjectMember> findMyProjectsByCursor(
            @Param("userId") Long userId,
            @Param("isPublic") Boolean isPublic,
            @Param("cursorJoinedAt") LocalDateTime cursorJoinedAt,
            @Param("cursorMemberId") Long cursorMemberId,
            Pageable pageable
    );

}
