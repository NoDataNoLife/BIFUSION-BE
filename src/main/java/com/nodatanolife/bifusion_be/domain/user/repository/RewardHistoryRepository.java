package com.nodatanolife.bifusion_be.domain.user.repository;

import com.nodatanolife.bifusion_be.domain.user.entity.RewardHistory;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RewardHistoryRepository extends JpaRepository<RewardHistory, Long> {

    void deleteByUser(User user);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM RewardHistory r WHERE r.user = :user")
    int sumAmountByUser(@Param("user") User user);
}
