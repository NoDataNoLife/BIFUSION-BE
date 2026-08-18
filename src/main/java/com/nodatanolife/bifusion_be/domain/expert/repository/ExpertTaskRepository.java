package com.nodatanolife.bifusion_be.domain.expert.repository;

import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertTask;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ExpertTaskRepository extends JpaRepository<ExpertTask, Long> {

    List<ExpertTask> findAllByExpert(User expert);

    @Query("SELECT t.status AS status, COUNT(t) AS count FROM ExpertTask t WHERE t.expert = :expert GROUP BY t.status")
    List<StatusCount> countGroupedByStatus(@Param("expert") User expert);

    @Query("SELECT AVG(t.satisfaction) FROM ExpertTask t WHERE t.expert = :expert AND t.satisfaction IS NOT NULL")
    BigDecimal findAverageSatisfaction(@Param("expert") User expert);

    @Query("SELECT t.expert.userId AS userId, COUNT(t) AS count FROM ExpertTask t WHERE t.status = :status GROUP BY t.expert.userId")
    List<ExpertCompletionCount> countGroupedByExpertForStatus(@Param("status") String status);

    interface StatusCount {
        String getStatus();
        Long getCount();
    }

    interface ExpertCompletionCount {
        Long getUserId();
        Long getCount();
    }
}
