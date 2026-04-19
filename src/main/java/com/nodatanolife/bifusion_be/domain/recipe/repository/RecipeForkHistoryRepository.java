package com.nodatanolife.bifusion_be.domain.recipe.repository;

import com.nodatanolife.bifusion_be.domain.recipe.entity.RecipeForkHistory;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecipeForkHistoryRepository extends JpaRepository<RecipeForkHistory, Long> {

    List<RecipeForkHistory> findAllByUser(User user);
}
