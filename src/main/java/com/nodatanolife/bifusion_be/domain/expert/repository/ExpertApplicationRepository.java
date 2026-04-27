package com.nodatanolife.bifusion_be.domain.expert.repository;

import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertApplication;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExpertApplicationRepository extends JpaRepository<ExpertApplication, Long> {

    void deleteByUser(User user);

    boolean existsByUserAndStatus(User user, String status);

    Optional<ExpertApplication> findByUserAndStatus(User user, String status);
}
