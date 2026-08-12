package com.nodatanolife.bifusion_be.domain.recipe.repository;

import com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    List<Recipe> findAllByUser(User user);

    Page<Recipe> findAllByIsPublicTrue(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Recipe> findAllByForkedFromIsNullAndUser_UserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Recipe> findAllByForkedFromIsNotNullAndUser_UserId(Long userId, Pageable pageable);
}
