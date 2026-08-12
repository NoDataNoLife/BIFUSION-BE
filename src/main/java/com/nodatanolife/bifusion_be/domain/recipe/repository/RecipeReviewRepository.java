package com.nodatanolife.bifusion_be.domain.recipe.repository;

import com.nodatanolife.bifusion_be.domain.recipe.entity.RecipeReview;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecipeReviewRepository extends JpaRepository<RecipeReview, Long> {

    List<RecipeReview> findAllByUser(User user);

    void deleteAllByParent(RecipeReview parent);
    
    Page<RecipeReview> findAllByRecipe_RecipeIdAndParentIsNull(Long recipeId, Pageable pageable);

    List<RecipeReview> findAllByParent_RecipeReviewIdInOrderByCreatedAtAsc(List<Long> parentIds);
}
