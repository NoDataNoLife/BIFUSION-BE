package com.nodatanolife.bifusion_be.domain.expert.repository;

import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertTask;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequest;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ExpertTaskRepository extends JpaRepository<ExpertTask, Long> {

    List<ExpertTask> findAllByExpert(User expert);

    // 파일 다운로드 권한 확인용. 이 대상들 중 하나의 검수를 배정받은 적이 있는지
    @Query("""
            SELECT COUNT(t) > 0 FROM ExpertTask t
            WHERE t.expert.userId = :expertId
              AND t.inspectionRequest.targetType = :targetType
              AND t.inspectionRequest.targetId IN :targetIds
            """)
    boolean existsAssignedTo(@Param("expertId") Long expertId,
                             @Param("targetType") InspectionTargetType targetType,
                             @Param("targetIds") List<Long> targetIds);

    Optional<ExpertTask> findByExpertTaskIdAndExpert(Long expertTaskId, User expert);

    /**
     * 상태를 바꾸는 작업(검수 시작·승인·반려, 신청자 취소)은 task row를 잠가 한 번에 하나만 반영되게 한다.
     * 취소가 먼저면 task가 지워져 이후 작업은 404, 시작·승인·반려가 먼저면 취소가 409로 막힌다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM ExpertTask t WHERE t.expertTaskId = :expertTaskId AND t.expert = :expert")
    Optional<ExpertTask> findForUpdate(@Param("expertTaskId") Long expertTaskId, @Param("expert") User expert);

    @Query("""
            SELECT t FROM ExpertTask t
            JOIN FETCH t.inspectionRequest v
            JOIN FETCH v.user
            WHERE t.expert = :expert
            ORDER BY t.updatedAt DESC
            """)
    List<ExpertTask> findMyReviewTasks(@Param("expert") User expert);

    @Query("SELECT t.status AS status, COUNT(t) AS count FROM ExpertTask t WHERE t.expert = :expert GROUP BY t.status")
    List<StatusCount> countGroupedByStatus(@Param("expert") User expert);

    @Query("SELECT AVG(t.satisfaction) FROM ExpertTask t WHERE t.expert = :expert AND t.satisfaction IS NOT NULL")
    BigDecimal findAverageSatisfaction(@Param("expert") User expert);

    @Query("SELECT t.expert.userId AS userId, COUNT(t) AS count FROM ExpertTask t WHERE t.status = :status GROUP BY t.expert.userId")
    List<ExpertCompletionCount> countGroupedByExpertForStatus(@Param("status") String status);

    /**
     * 자동 배정용 전문가별 업무량. activeCount는 아직 끝나지 않은(PENDING·IN_PROGRESS) task 수,
     * lastAssignedAt은 가장 최근 배정 시각. 배정 이력이 없는 전문가는 결과에 없다.
     */
    @Query("""
            SELECT t.expert.userId AS userId,
                   SUM(CASE WHEN t.status <> 'COMPLETED' THEN 1 ELSE 0 END) AS activeCount,
                   MAX(t.createdAt) AS lastAssignedAt
            FROM ExpertTask t
            WHERE t.expert.userId IN :expertIds
            GROUP BY t.expert.userId
            """)
    List<ExpertWorkload> findWorkloads(@Param("expertIds") List<Long> expertIds);

    // 신청의 현재 회차 task (반려·취소 후 재신청하면 회차마다 task가 새로 생긴다). 취소 시 잠가서 조회한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ExpertTask> findFirstByInspectionRequestOrderByExpertTaskIdDesc(InspectionRequest inspectionRequest);

    // 신청별 현재 회차 task (신청자 목록·상세용, 잠그지 않음)
    @Query("""
            SELECT t FROM ExpertTask t
            JOIN FETCH t.expert
            WHERE t.inspectionRequest IN :requests
              AND t.expertTaskId = (SELECT MAX(t2.expertTaskId) FROM ExpertTask t2 WHERE t2.inspectionRequest = t.inspectionRequest)
            """)
    List<ExpertTask> findLatestByInspectionRequestIn(@Param("requests") List<InspectionRequest> requests);

    interface StatusCount {
        String getStatus();
        Long getCount();
    }

    interface ExpertCompletionCount {
        Long getUserId();
        Long getCount();
    }

    interface ExpertWorkload {
        Long getUserId();
        Long getActiveCount();
        LocalDateTime getLastAssignedAt();
    }
}
