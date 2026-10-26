package com.nodatanolife.bifusion_be.domain.recipe.repository;

import com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe;
import com.nodatanolife.bifusion_be.domain.recipe.entity.RecipeForkHistory;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecipeForkHistoryRepository extends JpaRepository<RecipeForkHistory, Long> {

    // 회원 탈퇴 익명화 처리 대상 조회
    List<RecipeForkHistory> findAllByUser(User user);

    /**
     * 동일 사용자가 같은 원본 레시피를 이미 포크했는지 확인한다.
     * true면 RecipeForkServiceImpl#forkRecipe에서 ALREADY_FORKED_RECIPE 예외를 던져 중복 포크를 막는다.
     */
    boolean existsByUserAndOriginalRecipe(User user, Recipe originalRecipe);

    /**
     * 원본을 그대로 복제한 사본(POST /{recipeId}/fork)인지 확인한다. 포크 이력은 복제 경로에서만 남고,
     * 원본 내용을 불러와 다시 등록한 레시피(등록 API + forkedFromId)에는 남지 않는다.
     */
    boolean existsByForkedRecipe(Recipe forkedRecipe);

    // existsByForkedRecipe의 일괄 조회판 (목록 N+1 방지)
    @Query("SELECT h.forkedRecipe.recipeId FROM RecipeForkHistory h WHERE h.forkedRecipe.recipeId IN :recipeIds")
    List<Long> findForkedRecipeIdsIn(@Param("recipeIds") List<Long> recipeIds);
}
