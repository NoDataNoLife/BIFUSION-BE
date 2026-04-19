package com.nodatanolife.bifusion_be.domain.user.service;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.repository.RewardHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RewardHistoryServiceImpl implements RewardHistoryService {

    private final RewardHistoryRepository rewardHistoryRepository;

    /**
     * 회원 탈퇴 시 리워드 내역 삭제
     */
    @Override
    public void deleteByUser(User user) {
        rewardHistoryRepository.deleteByUser(user);
    }
}
