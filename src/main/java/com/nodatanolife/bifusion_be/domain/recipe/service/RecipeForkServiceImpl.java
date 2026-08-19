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

    /**
     * 레시피 포크(복제) 처리.
     * 1) 원본/요청자 존재 검증
     * 2) 비공개 원본이면 작성자 본인만 포크 가능하도록 접근 검증
     * 3) 동일 사용자의 중복 포크 방지
     * 4) Recipe.fork()로 비공개(isPublic=false) 사본 생성 후 저장
     * 5) 원본의 forkCount 증가
     * 6) 포크 이력(RecipeForkHistory) 기록
     */
    @Override
    @Transactional
    public RecipeForkResponse forkRecipe(Long recipeId, Long userId) {
        Recipe original = recipeRepository.findByRecipeIdAndDeletedAtIsNull(recipeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        // 비공개 레시피는 작성자 본인만 포크할 수 있다 (상세조회와 동일한 접근 규칙).
        // 이 체크가 없으면 ID만 알아도 남의 비공개 레시피 내용을 복제해 갈 수 있었다.
        if (!original.getIsPublic() && !original.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_RECIPE_ACCESS);
        }

        // 동일 사용자가 같은 원본을 이미 포크했다면 중복 생성을 막는다.
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