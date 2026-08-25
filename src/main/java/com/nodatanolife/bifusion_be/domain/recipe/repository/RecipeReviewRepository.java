package com.nodatanolife.bifusion_be.domain.recipe.repository;

import com.nodatanolife.bifusion_be.domain.recipe.entity.RecipeReview;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecipeReviewRepository extends JpaRepository<RecipeReview, Long> {

    // 회원 탈퇴 익명화 처리 대상 조회
    List<RecipeReview> findAllByUser(User user);

    // 최상위 댓글 삭제 시 딸린 대댓글을 일괄 하드 삭제하기 위한 메서드
    void deleteAllByParent(RecipeReview parent);

    // 레시피의 최상위 댓글만 페이지 조회 (parent가 없는 댓글)
    Page<RecipeReview> findAllByRecipe_RecipeIdAndParentIsNull(Long recipeId, Pageable pageable);

    // 여러 최상위 댓글 ID에 딸린 대댓글을 한 번에 조회 (댓글 목록 조회 시 N+1 방지용 배치 조회)
    List<RecipeReview> findAllByParent_RecipeReviewIdInOrderByCreatedAtAsc(List<Long> parentIds);

    /**
     * 마이페이지 "내 활동" 댓글 수 배치 집계용. 최상위 댓글 + 대댓글을 모두 합산한다.
     */
    @Query("SELECT rr.recipe.recipeId AS recipeId, COUNT(rr) AS count " +
            "FROM RecipeReview rr " +
            "WHERE rr.recipe.recipeId IN :recipeIds " +
            "GROUP BY rr.recipe.recipeId")
    List<RecipeReviewCount> countByRecipeIds(@Param("recipeIds") List<Long> recipeIds);

    interface RecipeReviewCount {
        Long getRecipeId();
        Long getCount();
    }
}
