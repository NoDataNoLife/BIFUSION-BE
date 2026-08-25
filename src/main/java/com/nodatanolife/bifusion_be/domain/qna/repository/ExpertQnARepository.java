package com.nodatanolife.bifusion_be.domain.qna.repository;

import com.nodatanolife.bifusion_be.domain.qna.entity.ExpertQnA;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ExpertQnARepository extends JpaRepository<ExpertQnA, Long> {

    List<ExpertQnA> findAllByUser(User user);

    @Query(
            value = "SELECT q FROM ExpertQnA q JOIN FETCH q.user " +
                    "WHERE q.isPublic = true " +
                    "AND q.deletedAt IS NULL " +
                    "AND (:keyword IS NULL OR q.title LIKE %:keyword%)",
            countQuery = "SELECT COUNT(q) FROM ExpertQnA q " +
                    "WHERE q.isPublic = true " +
                    "AND q.deletedAt IS NULL " +
                    "AND (:keyword IS NULL OR q.title LIKE %:keyword%)"
    )
    Page<ExpertQnA> findAllByCondition(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /**
     * 마이페이지 "내 활동" 커서 기반 조회용. 공개 여부와 무관하게 본인 소유 Q&A 전체를 대상으로,
     * 커서(createdAt, expertQnAId) 이후 데이터를 최신순으로 size+1개(hasNext 판단용) 조회한다.
     * hasCursor는 PostgreSQL이 "파라미터 IS NULL"만으로는 파라미터 타입을 추론하지 못해
     * "could not determine data type of parameter"로 실패하는 것을 피하기 위한 우회 파라미터다.
     */
    @Query("SELECT q FROM ExpertQnA q WHERE q.user.userId = :userId AND q.deletedAt IS NULL " +
            "AND (:hasCursor = false OR q.createdAt < :cursorCreatedAt " +
            "OR (q.createdAt = :cursorCreatedAt AND q.expertQnAId < :cursorId)) " +
            "ORDER BY q.createdAt DESC, q.expertQnAId DESC")
    List<ExpertQnA> findMyActivities(
            @Param("userId") Long userId,
            @Param("hasCursor") boolean hasCursor,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}
