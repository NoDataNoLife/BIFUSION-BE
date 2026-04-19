package com.nodatanolife.bifusion_be.domain.expert.service;

import com.nodatanolife.bifusion_be.domain.expert.repository.ExpertApplicationRepository;
import com.nodatanolife.bifusion_be.domain.expert.repository.ExpertTaskRepository;
import com.nodatanolife.bifusion_be.domain.expert.repository.ExpertVerificationRepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpertServiceImpl implements ExpertService {

    private final ExpertApplicationRepository expertApplicationRepository;
    private final ExpertTaskRepository expertTaskRepository;
    private final ExpertVerificationRepository expertVerificationRepository;

    /**
     * 회원 탈퇴 시 전문가 신청 내역 삭제
     */
    @Override
    public void deleteApplicationByUser(User user) {
        expertApplicationRepository.deleteByUser(user);
    }

    /**
     * 회원 탈퇴 시 전문가 관련 데이터 익명화
     * ExpertTask, ExpertVerification의 전문가/검토자를 탈퇴 계정으로 변경
     */
    @Override
    public void anonymizeByUser(User user, User deletedUser) {
        expertTaskRepository.findAllByExpert(user)
                .forEach(t -> t.anonymize(deletedUser));
        expertVerificationRepository.findAllByUser(user)
                .forEach(v -> v.anonymize(deletedUser));
        expertVerificationRepository.findAllByReviewedBy(user)
                .forEach(v -> v.anonymizeReviewer(deletedUser));
    }
}
