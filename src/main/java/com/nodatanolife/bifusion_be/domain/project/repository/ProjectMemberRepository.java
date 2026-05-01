package com.nodatanolife.bifusion_be.domain.project.repository;

import com.nodatanolife.bifusion_be.domain.project.entity.ProjectMember;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    long countByProjectProjectIdAndRole(Long projectId, String role);

    void deleteByUser(User user);

    boolean existsByProjectProjectIdAndUserUserId(Long projectId, Long userId);

    Optional<ProjectMember> findByProjectProjectIdAndUserUserId(Long projectId, Long userId);
}
