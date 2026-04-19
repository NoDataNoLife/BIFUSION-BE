package com.nodatanolife.bifusion_be.domain.user.service;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    /**
     * 회원 탈퇴 시 구독 정보 삭제
     */
    @Override
    public void deleteByUser(User user) {
        subscriptionRepository.deleteByUser(user);
    }
}
