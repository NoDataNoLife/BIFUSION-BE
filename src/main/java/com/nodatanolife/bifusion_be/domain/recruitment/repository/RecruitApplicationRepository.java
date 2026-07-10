package com.nodatanolife.bifusion_be.domain.recruitment.repository;

import com.nodatanolife.bifusion_be.domain.recruitment.entity.RecruitApplication;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecruitApplicationRepository extends JpaRepository<RecruitApplication, Long> {

    List<RecruitApplication> findAllByUser(User user);

    boolean existsByRecruit_RecruitIdAndUser_UserId(Long recruitId, Long userId);

}
