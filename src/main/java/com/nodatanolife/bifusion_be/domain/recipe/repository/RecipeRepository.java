package com.nodatanolife.bifusion_be.domain.recipe.repository;

import com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    // 회원 탈퇴 익명화 처리 대상 조회 (공개 여부와 무관하게 해당 유저의 전체 레시피)
    List<Recipe> findAllByUser(User user);

    // 공개(isPublic = true) 레시피만 조회. 현재 미사용이며 searchRecipes로 대체됨
    Page<Recipe> findAllByIsPublicTrue(Pageable pageable);

    /**
     * "내 자산" 목록 중 MINE(직접 작성) 조회용. forkedFrom이 없는(직접 작성한) 레시피만 대상이며,
     * 공개 여부와 무관하게 본인 소유 레시피 전체를 대상으로 한다.
     */
    @EntityGraph(attributePaths = "user")
    Page<Recipe> findAllByForkedFromIsNullAndUser_UserIdAndDeletedAtIsNull(Long userId, Pageable pageable);

    /**
     * "내 자산" 목록 중 FORKED(포크해온 것) 조회용. forkedFrom이 있는 레시피만 대상.
     */
    @EntityGraph(attributePaths = "user")
    Page<Recipe> findAllByForkedFromIsNotNullAndUser_UserIdAndDeletedAtIsNull(Long userId, Pageable pageable);

    /**
     * 커뮤니티 목록 조회용. isPublic = true인 레시피만 대상으로 하여
     * 비공개(내 자산 전용/포크 사본) 레시피가 커뮤니티 목록에 노출되지 않도록 막는다.
     * keyword는 제목 또는 짧은 설명에 대해 대소문자 무시 부분 일치로 검색한다.
     */
    @Query("SELECT r FROM Recipe r WHERE r.isPublic = true AND r.deletedAt IS NULL AND" +
            "(:keyword IS NULL OR :keyword = '' OR LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(r.shortDescription) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Recipe> searchRecipes(@Param("keyword") String keyword, Pageable pageable);

    Optional<Recipe> findByRecipeIdAndDeletedAtIsNull(Long recipeId);

    /**
     * 마이페이지 "내 활동" 커서 기반 조회용. 공개 여부와 무관하게 본인 소유 레시피 전체를 대상으로,
     * 커서(createdAt, recipeId) 이후 데이터를 최신순으로 size+1개(hasNext 판단용) 조회한다.
     * hasCursor는 PostgreSQL이 "파라미터 IS NULL"만으로는 파라미터 타입을 추론하지 못해
     * "could not determine data type of parameter"로 실패하는 것을 피하기 위한 우회 파라미터다.
     */
    @Query("SELECT r FROM Recipe r WHERE r.user.userId = :userId AND r.deletedAt IS NULL " +
            "AND (:hasCursor = false OR r.createdAt < :cursorCreatedAt " +
            "OR (r.createdAt = :cursorCreatedAt AND r.recipeId < :cursorId)) " +
            "ORDER BY r.createdAt DESC, r.recipeId DESC")
    List<Recipe> findMyActivities(
            @Param("userId") Long userId,
            @Param("hasCursor") boolean hasCursor,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}
