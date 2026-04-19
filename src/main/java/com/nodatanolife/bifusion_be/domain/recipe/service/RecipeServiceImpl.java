package com.nodatanolife.bifusion_be.domain.recipe.service;

import com.nodatanolife.bifusion_be.domain.recipe.repository.RecipeForkHistoryRepository;
import com.nodatanolife.bifusion_be.domain.recipe.repository.RecipeRepository;
import com.nodatanolife.bifusion_be.domain.recipe.repository.RecipeReviewRepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;
    private final RecipeReviewRepository recipeReviewRepository;
    private final RecipeForkHistoryRepository recipeForkHistoryRepository;

    /**
     * 회원 탈퇴 시 레시피 익명화
     * Recipe, RecipeReview, RecipeForkHistory의 owner를 탈퇴 계정으로 변경
     */
    @Override
    public void anonymizeByUser(User user, User deletedUser) {
        recipeRepository.findAllByUser(user)
                .forEach(r -> r.anonymize(deletedUser));
        recipeReviewRepository.findAllByUser(user)
                .forEach(r -> r.anonymize(deletedUser));
        recipeForkHistoryRepository.findAllByUser(user)
                .forEach(f -> f.anonymize(deletedUser));
    }
}
