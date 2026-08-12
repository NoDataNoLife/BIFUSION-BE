package com.nodatanolife.bifusion_be.domain.recipe.service;

import com.nodatanolife.bifusion_be.domain.recipe.dto.response.MyRecipeListResponse;
import com.nodatanolife.bifusion_be.domain.recipe.dto.response.RecipeDetailResponse;
import com.nodatanolife.bifusion_be.domain.recipe.dto.request.RecipeRegisterRequest;
import com.nodatanolife.bifusion_be.domain.recipe.dto.response.RecipeRegisterResponse;
import com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe;
import com.nodatanolife.bifusion_be.domain.recipe.entity.RecipeListType;
import com.nodatanolife.bifusion_be.domain.recipe.repository.RecipeForkHistoryRepository;
import com.nodatanolife.bifusion_be.domain.recipe.repository.RecipeRepository;
import com.nodatanolife.bifusion_be.domain.recipe.repository.RecipeReviewRepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nodatanolife.bifusion_be.domain.recipe.dto.response.RecipeListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
@Transactional
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;
    private final RecipeReviewRepository recipeReviewRepository;
    private final RecipeForkHistoryRepository recipeForkHistoryRepository;
    private final UserRepository userRepository;

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


    @Override
    public RecipeListResponse getRecipeList(String sort, int page, int size) {
        Sort sortOption = switch (sort) {
            case "MOST_POPULAR" -> Sort.by(Sort.Direction.DESC, "likeCount");
            case "MOST_FORKED" -> Sort.by(Sort.Direction.DESC, "forkCount");
            default -> Sort.by(Sort.Direction.DESC, "createdAt"); // LATEST
        };

        Pageable pageable = PageRequest.of(page, size, sortOption);
        Page<Recipe> recipePage = recipeRepository.findAllByIsPublicTrue(pageable);

        return RecipeListResponse.of(recipePage);
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeDetailResponse getRecipeDetail(Long recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        return RecipeDetailResponse.from(recipe);
    }

    @Override
    public RecipeRegisterResponse registerRecipe(RecipeRegisterRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Recipe forkedFrom = null;
        if (request.forkedFromId() != null) {
            forkedFrom = recipeRepository.findById(request.forkedFromId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
            forkedFrom.increaseForkCount();
        }

        Recipe recipe = Recipe.create(request, user, forkedFrom);
        Recipe savedRecipe = recipeRepository.save(recipe);

        return RecipeRegisterResponse.from(savedRecipe);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MyRecipeListResponse> getMyRecipes(RecipeListType type, Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Recipe> recipePage = switch (type) {
            case MINE -> recipeRepository.findAllByForkedFromIsNullAndUser_UserId(userId, pageable);
            case FORKED -> recipeRepository.findAllByForkedFromIsNotNullAndUser_UserId(userId, pageable);
        };

        return recipePage.map(MyRecipeListResponse::from);
    }
}
