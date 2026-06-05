package com.nodatanolife.bifusion_be.domain.qna.repository;

import com.nodatanolife.bifusion_be.domain.qna.entity.ExpertQnA;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpertQnARepository extends JpaRepository<ExpertQnA, Long> {

    List<ExpertQnA> findAllByUser(User user);

    @Query(
            value = "SELECT q FROM ExpertQnA q JOIN FETCH q.user " +
                    "WHERE q.isPublic = true " +
                    "AND (:keyword IS NULL OR q.title LIKE %:keyword%)",
            countQuery = "SELECT COUNT(q) FROM ExpertQnA q " +
                    "WHERE q.isPublic = true " +
                    "AND (:keyword IS NULL OR q.title LIKE %:keyword%)"
    )
    Page<ExpertQnA> findAllByCondition(
            @Param("keyword") String keyword,
            Pageable pageable
    );

}
