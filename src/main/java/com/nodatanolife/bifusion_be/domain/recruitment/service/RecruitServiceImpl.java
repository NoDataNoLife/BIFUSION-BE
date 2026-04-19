package com.nodatanolife.bifusion_be.domain.recruitment.service;

import com.nodatanolife.bifusion_be.domain.recruitment.repository.RecruitApplicationRepository;
import com.nodatanolife.bifusion_be.domain.recruitment.repository.RecruitRepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RecruitServiceImpl implements RecruitService {

    private final RecruitRepository recruitRepository;
    private final RecruitApplicationRepository recruitApplicationRepository;

    /**
     * 회원 탈퇴 시 채용 공고 및 지원 내역 익명화
     * Recruit, RecruitApplication의 작성자/지원자를 탈퇴 계정으로 변경
     */
    @Override
    public void anonymizeByUser(User user, User deletedUser) {
        recruitRepository.findAllByUser(user)
                .forEach(r -> r.anonymize(deletedUser));
        recruitApplicationRepository.findAllByUser(user)
                .forEach(r -> r.anonymize(deletedUser));
    }
}
