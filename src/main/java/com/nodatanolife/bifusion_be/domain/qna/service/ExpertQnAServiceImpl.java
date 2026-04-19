package com.nodatanolife.bifusion_be.domain.qna.service;

import com.nodatanolife.bifusion_be.domain.qna.repository.ExpertQnARepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpertQnAServiceImpl implements ExpertQnAService {

    private final ExpertQnARepository expertQnARepository;

    /**
     * 회원 탈퇴 시 전문가 Q&A 익명화
     * ExpertQnA의 작성자를 탈퇴 계정으로 변경
     */
    @Override
    public void anonymizeByUser(User user, User deletedUser) {
        expertQnARepository.findAllByUser(user)
                .forEach(q -> q.anonymize(deletedUser));
    }
}
