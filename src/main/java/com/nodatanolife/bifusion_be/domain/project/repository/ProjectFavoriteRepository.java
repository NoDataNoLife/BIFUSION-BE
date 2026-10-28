package com.nodatanolife.bifusion_be.domain.project.repository;

import com.nodatanolife.bifusion_be.domain.project.entity.Project;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectFavorite;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectFavoriteRepository extends JpaRepository<ProjectFavorite, Long> {

    void deleteByUser(User user);

    void deleteByProject(Project project);
}
