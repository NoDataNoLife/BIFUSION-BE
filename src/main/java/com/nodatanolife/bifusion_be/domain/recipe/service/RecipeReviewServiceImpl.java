package com.nodatanolife.bifusion_be.domain.recipe.service;

import java.util.Map;
import java.util.stream.Collectors;
import com.nodatanolife.bifusion_be.domain.recipe.repository.RecipeReviewRepository;
import com.nodatanolife.bifusion_be.domain.notification.service.NotificationService;
import com.nodatanolife.bifusion_be.domain.notification.entity.NotificationType;
import com.nodatanolife.bifusion_be.domain.notification.entity.TargetType;
import com.nodatanolife.bifusion_be.domain.recipe.dto.request.RecipeReviewRequest;
import com.nodatanolife.bifusion_be.domain.recipe.dto.request.RecipeReviewUpdateRequest;
import com.nodatanolife.bifusion_be.domain.recipe.dto.response.RecipeReviewResponse;
import com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe;
import com.nodatanolife.bifusion_be.domain.recipe.entity.RecipeReview;
import com.nodatanolife.bifusion_be.domain.recipe.repository.RecipeRepository;
import com.nodatanolife.bifusion_be.domain.recipe.repository.RecipeReviewRepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RecipeReviewServiceImpl implements RecipeReviewService {

    private final RecipeReviewRepository recipeReviewRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * 댓글/대댓글 작성.
     * parentReviewId로 부모 댓글을 조회/검증한 뒤, 부모 유무에 따라 별점 필수 여부를 검증하고 저장한다.
     * 요청자는 인증된 유저이므로 존재 검증 없이 getReferenceById로 프록시만 참조한다.
     */
    @Override
    public RecipeReviewResponse createReview(Long recipeId, Long userId, RecipeReviewRequest request) {
        Recipe recipe = recipeRepository.findByRecipeIdAndDeletedAtIsNull(recipeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        RecipeReview parent = resolveParent(recipeId, request.parentReviewId());
        validateRating(parent, request.rating());

        User user = userRepository.getReferenceById(userId);

        RecipeReview review = RecipeReview.create(
                recipe, user, parent, request.ratingAsShort(), request.content()
        );

        RecipeReview saved = recipeReviewRepository.save(review);

        if (!recipe.getUser().getUserId().equals(userId)) {
            notificationService.create(
                    recipe.getUser(),
                    NotificationType.RECIPE_REVIEW,
                    "새로운 리뷰/댓글이 등록되었습니다",
                    user.getNickname() + "님이 \"" + recipe.getTitle() + "\"에 새로운 리뷰를 남겼습니다.",
                    TargetType.RECIPE,
                    recipe.getRecipeId()
            );
        }

        return RecipeReviewResponse.from(saved);


    }

    /**
     * 댓글/대댓글 삭제(하드 삭제).
     * 최상위 댓글을 삭제하는 경우, 딸린 대댓글을 먼저 일괄 삭제한 뒤 본인을 삭제한다
     * (FK 제약 위반을 피하기 위한 순서).
     */
    @Override
    public boolean deleteReview(Long recipeId, Long reviewId, Long userId) {
        RecipeReview review = getOwnedReview(recipeId, reviewId, userId);

        boolean isReply = review.isReply();

        // 최상위 댓글을 삭제하면 딸린 대댓글도 함께 하드 삭제한다.
        if (!isReply) {
            recipeReviewRepository.deleteAllByParent(review);
        }

        recipeReviewRepository.delete(review);

        return isReply;
    }

    /**
     * 댓글/대댓글 수정. 기존 별점 유무(parent 여부)를 기준으로 요청의 별점 유효성을 재검증한다.
     */
    @Override
    public RecipeReviewResponse updateReview(Long recipeId, Long reviewId, Long userId, RecipeReviewUpdateRequest request) {
        RecipeReview review = getOwnedReview(recipeId, reviewId, userId);

        validateRating(review.getParent(), request.rating());

        review.update(request.ratingAsShort(), request.content());

        return RecipeReviewResponse.from(review);
    }

    /**
     * 최상위 댓글을 페이지 조회한 뒤, 해당 페이지에 포함된 댓글들의 대댓글을 한 번에 배치 조회해
     * 부모 ID 기준으로 그룹핑한다 (N+1 방지).
     */
    @Override
    @Transactional(readOnly = true)
    public Page<RecipeReviewResponse> getReviews(Long recipeId, int page, int size) {
        recipeRepository.findByRecipeIdAndDeletedAtIsNull(recipeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<RecipeReview> topLevelReviews = recipeReviewRepository.findAllByRecipe_RecipeIdAndParentIsNull(recipeId, pageable);

        List<Long> topLevelIds = topLevelReviews.getContent().stream()
                .map(RecipeReview::getRecipeReviewId)
                .toList();

        Map<Long, List<RecipeReviewResponse>> repliesByParentId = topLevelIds.isEmpty()
                ? Map.of()
                : recipeReviewRepository.findAllByParent_RecipeReviewIdInOrderByCreatedAtAsc(topLevelIds).stream()
                        .map(RecipeReviewResponse::from)
                        .collect(Collectors.groupingBy(RecipeReviewResponse::parentReviewId));

        return topLevelReviews.map(review ->
                RecipeReviewResponse.from(review, repliesByParentId.getOrDefault(review.getRecipeReviewId(), List.of()))
        );

    }

    /**
     * reviewId로 댓글을 조회하고, recipeId 일치 여부와 작성자 본인 여부를 함께 검증한다.
     * 수정/삭제 두 API가 공통으로 사용하는 소유권 검증 헬퍼.
     */
    private RecipeReview getOwnedReview(Long recipeId, Long reviewId, Long userId) {
        RecipeReview review = recipeReviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECIPE_REVIEW_NOT_FOUND));

        if (!review.getRecipe().getRecipeId().equals(recipeId)) {
            throw new BusinessException(ErrorCode.RECIPE_REVIEW_NOT_FOUND);
        }
        if (!review.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.RECIPE_REVIEW_ACCESS_DENIED);
        }

        return review;
    }

    /**
     * parentReviewId로 부모 댓글을 조회하고 유효성을 검증한다. null이면 최상위 댓글이므로 null 반환.
     */
    private RecipeReview resolveParent(Long recipeId, Long parentReviewId) {
        if (parentReviewId == null) {
            return null;
        }

        RecipeReview parent = recipeReviewRepository.findById(parentReviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARENT_REVIEW_NOT_FOUND));

        // 다른 레시피의 리뷰 ID를 잘못 넣는 경우를 막는다.
        if (!parent.getRecipe().getRecipeId().equals(recipeId)) {
            throw new BusinessException(ErrorCode.PARENT_REVIEW_RECIPE_MISMATCH);
        }

        // 대댓글에는 다시 답글을 달 수 없다.
        if (parent.isReply()) {
            throw new BusinessException(ErrorCode.REVIEW_REPLY_DEPTH_EXCEEDED);
        }

        return parent;
    }

    // 별점은 최상위 리뷰만 가진다. parent가 없으면 별점 필수, 있으면 별점 금지.
    private void validateRating(RecipeReview parent, Integer rating) {
        if (parent == null && rating == null) {
            throw new BusinessException(ErrorCode.REVIEW_RATING_REQUIRED);
        }
        if (parent != null && rating != null) {
            throw new BusinessException(ErrorCode.REPLY_RATING_NOT_ALLOWED);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> countCommentsByRecipeIds(List<Long> recipeIds) {
        if (recipeIds.isEmpty()) {
            return Map.of();
        }
        return recipeReviewRepository.countByRecipeIds(recipeIds).stream()
                .collect(Collectors.toMap(
                        RecipeReviewRepository.RecipeReviewCount::getRecipeId,
                        RecipeReviewRepository.RecipeReviewCount::getCount
                ));
    }


}