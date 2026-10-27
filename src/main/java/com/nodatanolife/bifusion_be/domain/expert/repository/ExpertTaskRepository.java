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
import java.util.List;
import java.util.Optional;

public interface ExpertTaskRepository extends JpaRepository<ExpertTask, Long> {

    List<ExpertTask> findAllByExpert(User expert);

    // 파일 다운로드 권한 확인용. 이 대상들 중 하나의 검수를 맡은 적이 있는지
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
     * 승인·반려는 task row를 잠가 한 번에 하나만 반영되게 한다.
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

    // 신청의 가장 최근 회차 task (반려 후 재신청하면 회차마다 task가 새로 생긴다). 시작·취소 판정 때 잠가서 조회한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ExpertTask> findFirstByInspectionRequestOrderByExpertTaskIdDesc(InspectionRequest inspectionRequest);

    // 신청별 가장 최근 회차 task (신청자 목록·상세용, 잠그지 않음). 재신청 후 아직 아무도 시작하지 않았으면 지난 회차 task다
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
}
