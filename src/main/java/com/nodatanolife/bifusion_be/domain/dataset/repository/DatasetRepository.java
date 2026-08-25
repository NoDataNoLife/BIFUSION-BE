package com.nodatanolife.bifusion_be.domain.dataset.repository;

import com.nodatanolife.bifusion_be.domain.dataset.entity.Dataset;
import com.nodatanolife.bifusion_be.domain.dataset.entity.DatasetType;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface DatasetRepository extends JpaRepository<Dataset, Long> {

    // 회원 탈퇴 익명화 처리 대상 조회 (공개 여부와 무관하게 해당 유저의 전체 데이터셋)
    List<Dataset> findAllByUser(User user);

    /**
     * "내 자산" 목록 조회용. 공개 여부와 무관하게 본인이 작성한 데이터셋만 type으로 필터링한다.
     * (본인 소유이므로 owner 검증만으로 충분하고 isPublic 조건은 필요 없다)
     */
    @EntityGraph(attributePaths = "user")
    @Query("SELECT d FROM Dataset d WHERE d.type = :type AND d.user.userId = :userId AND d.deletedAt IS NULL")
    Page<Dataset> findAllByTypeAndUser_UserId(@Param("type") DatasetType type, @Param("userId") Long userId, Pageable pageable);

    /**
     * 커뮤니티 목록 조회용. isPublic = true인 데이터셋만 대상으로 하여
     * 비공개(내 자산 전용) 데이터셋이 커뮤니티 목록에 노출되지 않도록 막는다.
     */
    @Query(
            value = "SELECT d FROM Dataset d JOIN FETCH d.user WHERE d.isPublic = true AND d.deletedAt IS NULL AND (:keyword IS NULL OR d.title LIKE %:keyword%)",
            countQuery = "SELECT COUNT(d) FROM Dataset d WHERE d.isPublic = true AND d.deletedAt IS NULL AND (:keyword IS NULL OR d.title LIKE %:keyword%)"
    )
    Page<Dataset> findAllByCondition(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 마이페이지 "내 활동" 커서 기반 조회용. 공개 여부와 무관하게 본인 소유 데이터셋 전체를 대상으로,
     * 커서(createdAt, datasetId) 이후 데이터를 최신순으로 size+1개(hasNext 판단용) 조회한다.
     * hasCursor는 PostgreSQL이 "파라미터 IS NULL"만으로는 파라미터 타입을 추론하지 못해
     * "could not determine data type of parameter"로 실패하는 것을 피하기 위한 우회 파라미터다.
     */
    @Query("SELECT d FROM Dataset d WHERE d.user.userId = :userId AND d.deletedAt IS NULL " +
            "AND (:hasCursor = false OR d.createdAt < :cursorCreatedAt " +
            "OR (d.createdAt = :cursorCreatedAt AND d.datasetId < :cursorId)) " +
            "ORDER BY d.createdAt DESC, d.datasetId DESC")
    List<Dataset> findMyActivities(
            @Param("userId") Long userId,
            @Param("hasCursor") boolean hasCursor,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}
