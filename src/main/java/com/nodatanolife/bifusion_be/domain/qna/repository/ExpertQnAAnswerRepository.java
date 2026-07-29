package com.nodatanolife.bifusion_be.domain.qna.repository;

import com.nodatanolife.bifusion_be.domain.qna.entity.ExpertQnA;
import com.nodatanolife.bifusion_be.domain.qna.entity.ExpertQnAAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpertQnAAnswerRepository extends JpaRepository<ExpertQnAAnswer, Long> {
    List<ExpertQnAAnswer> findAllByExpertQnAOrderByCreatedAtAsc(ExpertQnA expertQnA);
}