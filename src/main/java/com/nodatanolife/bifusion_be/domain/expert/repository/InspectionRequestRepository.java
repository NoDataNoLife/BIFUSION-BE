package com.nodatanolife.bifusion_be.domain.expert.repository;

import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequest;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequestStatus;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * 신청자의 "내가 요청한 검수" 목록. 취소된 신청은 빼고 신청일 최신순으로 페이지 조회한다.
     * - 현재 회차 task(가장 큰 expertTaskId)만 붙여 진행 상태(InspectionProgressStatus)를 계산한다.
     * - keyword는 대상 이름(레시피·데이터셋 제목, 증강 데이터는 프로젝트명)에서 찾는다. 대상은 targetType별로 LEFT JOIN한다.
     * - status는 InspectionProgressStatus 이름. null이면 전체
     */
    @Query("""
            SELECT r FROM InspectionRequest r
            LEFT JOIN ExpertTask t ON t.inspectionRequest = r
            LEFT JOIN Recipe rc ON r.targetType = com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType.RECIPE
                 AND rc.recipeId = r.targetId
            LEFT JOIN Dataset ds ON r.targetType = com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType.UPLOADED_DATA
                 AND ds.datasetId = r.targetId
            LEFT JOIN AugmentationConfig ac ON r.targetType = com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType.AUGMENTED_DATA
                 AND ac.augmentationConfigId = r.targetId
            LEFT JOIN ac.job j
            LEFT JOIN j.project p
            WHERE r.user = :user
              AND r.status <> :cancelled
              AND (t.expertTaskId IS NULL
                   OR t.expertTaskId = (SELECT MAX(t2.expertTaskId) FROM ExpertTask t2 WHERE t2.inspectionRequest = r))
              AND (:targetType IS NULL OR r.targetType = :targetType)
              AND (:keyword IS NULL
                   OR LOWER(COALESCE(rc.title, ds.title, p.title)) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
              AND (:status IS NULL
                   OR (CASE WHEN r.status = :approved THEN 'APPROVED'
                            WHEN r.status = :rejected THEN 'REJECTED'
                            WHEN t.status = 'IN_PROGRESS' THEN 'IN_PROGRESS'
                            ELSE 'PENDING' END) = CAST(:status AS string))
            ORDER BY r.appliedAt DESC, r.inspectionRequestId DESC
            """)
    Page<InspectionRequest> findMyRequests(@Param("user") User user,
                                           @Param("targetType") InspectionTargetType targetType,
                                           @Param("keyword") String keyword,
                                           @Param("status") String status,
                                           @Param("cancelled") InspectionRequestStatus cancelled,
                                           @Param("approved") InspectionRequestStatus approved,
                                           @Param("rejected") InspectionRequestStatus rejected,
                                           Pageable pageable);

    // 신청자 목록 상단 요약용. 취소 건을 뺀 본인 신청을 (신청 상태, 현재 회차 task 상태) 조합별로 센다.
    @Query("""
            SELECT r.status AS requestStatus, t.status AS taskStatus, COUNT(r) AS count
            FROM InspectionRequest r
            LEFT JOIN ExpertTask t ON t.inspectionRequest = r
            WHERE r.user = :user
              AND r.status <> :cancelled
              AND (t.expertTaskId IS NULL
                   OR t.expertTaskId = (SELECT MAX(t2.expertTaskId) FROM ExpertTask t2 WHERE t2.inspectionRequest = r))
            GROUP BY r.status, t.status
            """)
    List<ProgressCount> countMyRequestsByProgress(@Param("user") User user,
                                                  @Param("cancelled") InspectionRequestStatus cancelled);

    interface ProgressCount {
        InspectionRequestStatus getRequestStatus();
        String getTaskStatus();
        Long getCount();
    }

    // 전문가 자동 배정 도입 전에 만들어져 배정(ExpertTask) 없이 대기 중으로 남은 신청 (일회성 정리용)
    @Query("""
            SELECT r.inspectionRequestId FROM InspectionRequest r
            WHERE r.status = :status
              AND NOT EXISTS (SELECT 1 FROM ExpertTask t WHERE t.inspectionRequest = r)
            """)
    List<Long> findIdsWithoutTaskByStatus(@Param("status") InspectionRequestStatus status);
}
