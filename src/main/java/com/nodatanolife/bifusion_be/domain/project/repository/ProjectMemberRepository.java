package com.nodatanolife.bifusion_be.domain.project.repository;

import com.nodatanolife.bifusion_be.domain.project.entity.ProjectMember;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectRole;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    long countByProjectProjectIdAndRole(Long projectId, ProjectRole role);

    void deleteByUser(User user);

    boolean existsByProjectProjectIdAndUserUserId(Long projectId, Long userId);

    Optional<ProjectMember> findByProjectProjectIdAndUserUserId(Long projectId, Long userId);

    List<ProjectMember> findByProjectProjectId(Long projectId);

    @Query("select pm from ProjectMember pm join fetch pm.project where pm.user.userId = :userId")
    List<ProjectMember> findAllByUserIdWithProject(@Param("userId") Long userId);

}
