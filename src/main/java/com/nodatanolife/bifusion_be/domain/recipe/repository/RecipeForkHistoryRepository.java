package com.nodatanolife.bifusion_be.domain.recipe.repository;

import com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe;
import com.nodatanolife.bifusion_be.domain.recipe.entity.RecipeForkHistory;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecipeForkHistoryRepository extends JpaRepository<RecipeForkHistory, Long> {

    // 회원 탈퇴 익명화 처리 대상 조회
    List<RecipeForkHistory> findAllByUser(User user);

    /**
     * 동일 사용자가 같은 원본 레시피를 이미 포크했는지 확인한다.
     * true면 RecipeForkServiceImpl#forkRecipe에서 ALREADY_FORKED_RECIPE 예외를 던져 중복 포크를 막는다.
     */
    boolean existsByUserAndOriginalRecipe(User user, Recipe originalRecipe);
}
