package com.nodatanolife.bifusion_be.domain.expert.repository;

import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequest;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequestStatus;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InspectionRequestRepository extends JpaRepository<InspectionRequest, Long> {

    List<InspectionRequest> findAllByUser(User user);

    List<InspectionRequest> findAllByReviewedBy(User reviewedBy);

    Optional<InspectionRequest> findByUserAndTargetTypeAndTargetId(User user, InspectionTargetType targetType, Long targetId);

    // 재신청(기존 row 재사용)이 동시에 들어와 task가 두 개 배정되지 않도록 기존 신청 row를 잠근다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM InspectionRequest r WHERE r.user = :user AND r.targetType = :targetType AND r.targetId = :targetId")
    Optional<InspectionRequest> findForUpdate(@Param("user") User user,
                                              @Param("targetType") InspectionTargetType targetType,
                                              @Param("targetId") Long targetId);

    Optional<InspectionRequest> findFirstByTargetTypeAndTargetIdOrderByAppliedAtDesc(InspectionTargetType targetType, Long targetId);

    List<InspectionRequest> findAllByTargetTypeAndTargetIdIn(InspectionTargetType targetType, List<Long> targetIds);

    // 전문가 자동 배정 도입 전에 만들어져 배정(ExpertTask) 없이 대기 중으로 남은 신청 (일회성 정리용)
    @Query("""
            SELECT r.inspectionRequestId FROM InspectionRequest r
            WHERE r.status = :status
              AND NOT EXISTS (SELECT 1 FROM ExpertTask t WHERE t.inspectionRequest = r)
            """)
    List<Long> findIdsWithoutTaskByStatus(@Param("status") InspectionRequestStatus status);
}
