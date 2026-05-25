package com.nodatanolife.bifusion_be.domain.qna.repository;

import com.nodatanolife.bifusion_be.domain.qna.entity.ExpertQnAAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpertQnAAnswerRepository extends JpaRepository<ExpertQnAAnswer, Long> {
}