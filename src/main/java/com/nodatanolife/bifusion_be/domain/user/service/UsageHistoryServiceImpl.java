package com.nodatanolife.bifusion_be.domain.user.service;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.repository.UsageHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UsageHistoryServiceImpl implements UsageHistoryService {

    private final UsageHistoryRepository usageHistoryRepository;

    /**
     * 회원 탈퇴 시 사용 내역 삭제
     */
    @Override
    public void deleteByUser(User user) {
        usageHistoryRepository.deleteByUser(user);
    }
}
