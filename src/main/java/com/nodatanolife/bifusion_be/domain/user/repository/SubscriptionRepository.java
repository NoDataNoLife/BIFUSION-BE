package com.nodatanolife.bifusion_be.domain.user.repository;

import com.nodatanolife.bifusion_be.domain.user.entity.Subscription;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    void deleteByUser(User user);
}
