package com.nodatanolife.bifusion_be.domain.activityLog.service;

import com.nodatanolife.bifusion_be.domain.activityLog.repository.ActivityLogRepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ActivityLogServiceImpl implements ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    /**
     * 회원 탈퇴 시 활동 로그 익명화
     * ActivityLog의 작성자를 탈퇴 계정으로 변경
     */
    @Override
    public void anonymizeByUser(User user, User deletedUser) {
        activityLogRepository.findAllByUser(user)
                .forEach(a -> a.anonymize(deletedUser));
    }
}
