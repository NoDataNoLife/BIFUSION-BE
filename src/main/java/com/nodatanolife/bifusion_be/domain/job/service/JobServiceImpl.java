package com.nodatanolife.bifusion_be.domain.job.service;

import com.nodatanolife.bifusion_be.domain.job.repository.JobRepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;

    /**
     * 회원 탈퇴 시 구직 정보 익명화
     * Job의 작성자를 탈퇴 계정으로 변경
     */
    @Override
    public void anonymizeByUser(User user, User deletedUser) {
        jobRepository.findAllByUser(user)
                .forEach(j -> j.anonymize(deletedUser));
    }
}
