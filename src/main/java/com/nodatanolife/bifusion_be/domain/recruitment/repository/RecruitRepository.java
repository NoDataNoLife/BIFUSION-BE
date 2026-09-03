package com.nodatanolife.bifusion_be.domain.recruitment.repository;

import com.nodatanolife.bifusion_be.domain.recruitment.entity.Recruit;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RecruitRepository extends JpaRepository<Recruit, Long> {

    List<Recruit> findAllByUser(User user);

    @Query("SELECT r FROM Recruit r WHERE r.isPublic = true AND r.deletedAt IS NULL")
    Page<Recruit> findAllByIsPublicTrue(Pageable pageable);

    /**
     * 마이페이지 "내 활동" 커서 기반 조회용. 공개 여부와 무관하게 본인 소유 팀원 모집 게시글 전체를 대상으로,
     * 커서(createdAt, recruitId) 이후 데이터를 최신순으로 size+1개(hasNext 판단용) 조회한다.
     * hasCursor는 PostgreSQL이 "파라미터 IS NULL"만으로는 파라미터 타입을 추론하지 못해
     * "could not determine data type of parameter"로 실패하는 것을 피하기 위한 우회 파라미터다.
     */
    @Query("SELECT r FROM Recruit r WHERE r.user.userId = :userId AND r.deletedAt IS NULL " +
            "AND (:hasCursor = false OR r.createdAt < :cursorCreatedAt " +
            "OR (r.createdAt = :cursorCreatedAt AND r.recruitId < :cursorId)) " +
            "ORDER BY r.createdAt DESC, r.recruitId DESC")
    List<Recruit> findMyActivities(
            @Param("userId") Long userId,
            @Param("hasCursor") boolean hasCursor,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("SELECT r FROM Recruit r JOIN FETCH r.user WHERE r.user.userId = :userId AND r.isPublic = true AND r.isMyPageVisible = true AND r.deletedAt IS NULL ORDER BY r.createdAt DESC")
    List<Recruit> findPublicByUserId(@Param("userId") Long userId);
}
