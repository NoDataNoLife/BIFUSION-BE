package com.nodatanolife.bifusion_be.domain.expert.repository;

import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertVerification;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpertVerificationRepository extends JpaRepository<ExpertVerification, Long> {

    List<ExpertVerification> findAllByUser(User user);

    List<ExpertVerification> findAllByReviewedBy(User reviewedBy);

    boolean existsByUserAndTargetTypeAndTargetId(User user, InspectionTargetType targetType, Long targetId);
}
