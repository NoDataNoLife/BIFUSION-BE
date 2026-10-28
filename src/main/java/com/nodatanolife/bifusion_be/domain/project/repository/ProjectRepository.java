package com.nodatanolife.bifusion_be.domain.project.repository;

import com.nodatanolife.bifusion_be.domain.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    // 삭제(soft delete)된 프로젝트는 없는 것으로 본다
    Optional<Project> findByProjectIdAndDeletedAtIsNull(Long projectId);
}
