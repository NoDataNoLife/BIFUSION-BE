package com.nodatanolife.bifusion_be.domain.expert.repository;

import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertApplication;
import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertApplicationStatus;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExpertApplicationRepository extends JpaRepository<ExpertApplication, Long> {

    void deleteByUser(User user);

    boolean existsByUserAndStatus(User user, ExpertApplicationStatus status);

    Optional<ExpertApplication> findByUserAndStatus(User user, ExpertApplicationStatus status);

    Optional<ExpertApplication> findFirstByUserOrderByAppliedAtDesc(User user);

    List<ExpertApplication> findAllByStatus(ExpertApplicationStatus status);

    // 자동 배정 후보: 전문가 인증(APPROVED)을 받았고 탈퇴하지 않은 사용자
    @Query("SELECT DISTINCT a.user.userId FROM ExpertApplication a WHERE a.status = :status AND a.user.role <> 'DELETED'")
    List<Long> findActiveUserIdsByStatus(@Param("status") ExpertApplicationStatus status);
}
