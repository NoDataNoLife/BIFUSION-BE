package com.nodatanolife.bifusion_be.domain.qna.repository;

import com.nodatanolife.bifusion_be.domain.qna.entity.ExpertQnA;
import com.nodatanolife.bifusion_be.domain.qna.entity.ExpertQnAAnswer;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpertQnAAnswerRepository extends JpaRepository<ExpertQnAAnswer, Long> {
    List<ExpertQnAAnswer> findAllByExpertQnAAndDeletedAtIsNullOrderByCreatedAtAsc(ExpertQnA expertQnA);
    // 답변 보상은 질문 하나당 전문가 한 명에게 1회
    boolean existsByExpertQnAAndUser(ExpertQnA expertQnA, User user);

}