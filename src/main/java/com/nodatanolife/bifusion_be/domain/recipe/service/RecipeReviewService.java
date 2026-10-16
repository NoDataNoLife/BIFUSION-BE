package com.nodatanolife.bifusion_be.domain.recipe.service;

import java.util.List;
import java.util.Map;
import com.nodatanolife.bifusion_be.domain.recipe.dto.request.RecipeReviewRequest;
import com.nodatanolife.bifusion_be.domain.recipe.dto.request.RecipeReviewUpdateRequest;
import com.nodatanolife.bifusion_be.domain.recipe.dto.response.RecipeReviewResponse;
import org.springframework.data.domain.Page;

public interface RecipeReviewService {

    /**
     * 댓글/대댓글을 작성한다. parentReviewId가 null이면 최상위 댓글(별점 필수),
     * 값이 있으면 해당 댓글의 대댓글(별점 불가)로 등록된다.
     */
    RecipeReviewResponse createReview(Long recipeId, Long userId, RecipeReviewRequest request);

    /**
     * 댓글/대댓글을 삭제한다. 최상위 댓글을 삭제하면 딸린 대댓글도 함께 삭제되며,
     * 반환값은 삭제 대상이 대댓글이었는지 여부다.
     */
    boolean deleteReview(Long recipeId, Long reviewId, Long userId);

    /**
     * 댓글/대댓글을 수정한다. 작성자 본인만 수정할 수 있다.
     */
    RecipeReviewResponse updateReview(Long recipeId, Long reviewId, Long userId, RecipeReviewUpdateRequest request);

    /**
     * 레시피의 최상위 댓글을 페이지 조회한다. 각 댓글에는 딸린 대댓글이 함께 채워진다.
     */
    Page<RecipeReviewResponse> getReviews(Long recipeId, int page, int size);

    Map<Long, Long> countCommentsByRecipeIds(List<Long> recipeIds);
}
