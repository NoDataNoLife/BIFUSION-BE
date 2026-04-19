package com.nodatanolife.bifusion_be.domain.project.repository;

import com.nodatanolife.bifusion_be.domain.project.entity.ProjectMember;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    void deleteByUser(User user);
}
