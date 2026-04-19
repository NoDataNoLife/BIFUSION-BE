package com.nodatanolife.bifusion_be.domain.qna.repository;

import com.nodatanolife.bifusion_be.domain.qna.entity.ExpertQnA;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpertQnARepository extends JpaRepository<ExpertQnA, Long> {

    List<ExpertQnA> findAllByUser(User user);
}
