package com.nodatanolife.bifusion_be.domain.activityLog.repository;

import com.nodatanolife.bifusion_be.domain.activityLog.entity.ActivityLog;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findAllByUser(User user);
}
