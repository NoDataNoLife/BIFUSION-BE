package com.nodatanolife.bifusion_be.domain.expert.repository;

import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequest;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequestStatus;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InspectionRequestRepository extends JpaRepository<InspectionRequest, Long> {

    List<InspectionRequest> findAllByUser(User user);

    List<InspectionRequest> findAllByReviewedBy(User reviewedBy);

    Optional<InspectionRequest> findByUserAndTargetTypeAndTargetId(User user, InspectionTargetType targetType, Long targetId);

    Optional<InspectionRequest> findFirstByTargetTypeAndTargetIdOrderByAppliedAtDesc(InspectionTargetType targetType, Long targetId);

    List<InspectionRequest> findAllByTargetTypeAndTargetIdIn(InspectionTargetType targetType, List<Long> targetIds);

    boolean existsByUserAndTargetTypeAndTargetIdAndStatus(
            User user,
            InspectionTargetType targetType,
            Long targetId,
            InspectionRequestStatus status
    );
}
