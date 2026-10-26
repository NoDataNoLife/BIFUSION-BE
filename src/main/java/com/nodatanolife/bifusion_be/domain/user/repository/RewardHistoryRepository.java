package com.nodatanolife.bifusion_be.domain.user.repository;

import com.nodatanolife.bifusion_be.domain.user.entity.RewardHistory;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RewardHistoryRepository extends JpaRepository<RewardHistory, Long> {

    void deleteByUser(User user);

    // 지정한 종류의 내역만 합산 (전문가 성과의 총 리워드는 전문가 활동 보상만 센다)
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM RewardHistory r WHERE r.user = :user AND r.type IN :types")
    int sumAmountByUserAndTypeIn(@Param("user") User user, @Param("types") List<String> types);
}
