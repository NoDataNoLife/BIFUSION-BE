package com.nodatanolife.bifusion_be.domain.qna.service;

import com.nodatanolife.bifusion_be.domain.qna.dto.request.ExpertQnAAnswerRequest;
import com.nodatanolife.bifusion_be.domain.qna.dto.response.ExpertQnAAnswerResponse;
import com.nodatanolife.bifusion_be.domain.qna.entity.ExpertQnA;
import com.nodatanolife.bifusion_be.domain.qna.entity.ExpertQnAAnswer;
import com.nodatanolife.bifusion_be.domain.qna.repository.ExpertQnAAnswerRepository;
import com.nodatanolife.bifusion_be.domain.qna.repository.ExpertQnARepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpertQnAServiceImpl implements ExpertQnAService {

    private final ExpertQnARepository expertQnARepository;
    private final ExpertQnAAnswerRepository expertQnAAnswerRepository;
    private final UserRepository userRepository;

    /**
     * 회원 탈퇴 시 전문가 Q&A 익명화
     * ExpertQnA의 작성자를 탈퇴 계정으로 변경
     */
    @Override
    public void anonymizeByUser(User user, User deletedUser) {
        expertQnARepository.findAllByUser(user)
                .forEach(q -> q.anonymize(deletedUser));
    }

    @Override
    public ExpertQnAAnswerResponse createAnswer(Long userId, Long qnaId, ExpertQnAAnswerRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!user.getIsExpert()) {
            throw new BusinessException(ErrorCode.EXPERT_ONLY);
        }

        ExpertQnA qna = expertQnARepository.findById(qnaId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOARD_NOT_FOUND));

        ExpertQnAAnswer answer = ExpertQnAAnswer.create(qna, user, request.getContent());
        expertQnAAnswerRepository.save(answer);

        return ExpertQnAAnswerResponse.from(answer);
    }

}
