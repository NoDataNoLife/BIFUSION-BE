package com.nodatanolife.bifusion_be.domain.recipe.service;

import com.nodatanolife.bifusion_be.domain.recipe.dto.response.RecipeForkResponse;
import com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe;
import com.nodatanolife.bifusion_be.domain.recipe.entity.RecipeForkHistory;
import com.nodatanolife.bifusion_be.domain.recipe.repository.RecipeForkHistoryRepository;
import com.nodatanolife.bifusion_be.domain.recipe.repository.RecipeRepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecipeForkServiceImpl implements RecipeForkService {

    private final RecipeRepository recipeRepository;
    private final RecipeForkHistoryRepository recipeForkHistoryRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public RecipeForkResponse forkRecipe(Long recipeId, Long userId) {
        Recipe original = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        boolean alreadyForked = recipeForkHistoryRepository
                .existsByUserAndOriginalRecipe(user, original);
        if (alreadyForked) {
            throw new BusinessException(ErrorCode.ALREADY_FORKED_RECIPE);
        }

        Recipe forked = Recipe.fork(original, user);
        Recipe savedForked = recipeRepository.save(forked);

        original.increaseForkCount();

        RecipeForkHistory history = RecipeForkHistory.create(user, savedForked, original, original.getTitle());
        recipeForkHistoryRepository.save(history);

        return RecipeForkResponse.of(savedForked.getRecipeId(), original.getRecipeId());
    }
}