package com.nodatanolife.bifusion_be.domain.recipe.service;

import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionSummary;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionTargetResponse;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;
import com.nodatanolife.bifusion_be.domain.expert.service.InspectionResultService;
import com.nodatanolife.bifusion_be.domain.expert.service.InspectionService;
import com.nodatanolife.bifusion_be.domain.file.entity.File;
import com.nodatanolife.bifusion_be.domain.file.repository.FileRepository;
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
import java.math.BigDecimal;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;
    private final InspectionResultService inspectionResultService;
    private final InspectionService inspectionService;
    private final RecipeReviewRepository recipeReviewRepository;
    private final RecipeForkHistoryRepository recipeForkHistoryRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;

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


    /**
     * 레시피 상세 조회.
     * 비공개 레시피인데 요청자가 작성자 본인이 아니면 접근을 차단한다.
     * (커뮤니티/내 자산 상세 화면이 동일 API를 공유하므로 이 가드가 유일한 비공개 방어선이다)
     */
    @Override
    @Transactional(readOnly = true)
    public RecipeDetailResponse getRecipeDetail(Long recipeId, Long userId) {
        Recipe recipe = recipeRepository.findByRecipeIdAndDeletedAtIsNull(recipeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!recipe.getIsPublic() && !recipe.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_RECIPE_ACCESS);
        }

        InspectionSummary verificationSummary = inspectionResultService
                .getSummary(InspectionTargetType.RECIPE, recipeId);
        // 최상위 리뷰 + 대댓글 합산 (마이페이지·공개 프로필 commentCount와 같은 기준).
        // 리뷰 목록 API의 totalElements는 최상위 리뷰 페이지 기준이라 이 값과 다르다.
        long reviewCount = recipeReviewRepository.countByRecipe_RecipeId(recipeId);
        // 검수 신청 정보(신청 ID 포함)는 작성자 본인에게만 내려준다
        InspectionTargetResponse inspectionTarget = recipe.getUser().getUserId().equals(userId)
                ? inspectionResultService.getRecipeTargets(userId, List.of(recipe)).get(recipeId)
                : null;

        return RecipeDetailResponse.from(recipe, verificationSummary, reviewCount, inspectionTarget);
    }

    /**
     * 레시피 등록. forkedFromId가 있으면 원본 레시피를 조회해 forkCount를 증가시키고
     * 포크 등록으로 처리한다(단, 이 경로는 forkedFromId를 직접 넣는 "등록형" 포크이며,
     * 원본을 그대로 복제하는 "복제형" 포크는 RecipeForkServiceImpl#forkRecipe를 사용한다).
     * isPublic은 request 값을 그대로 반영한다(null이면 Recipe.create 내부에서 공개로 기본 처리).
     */
    @Override
    public RecipeRegisterResponse registerRecipe(RecipeRegisterRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Recipe forkedFrom = null;
        if (request.forkedFromId() != null) {
            forkedFrom = recipeRepository.findByRecipeIdAndDeletedAtIsNull(request.forkedFromId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
            forkedFrom.increaseForkCount();
        }

        Recipe recipe = Recipe.create(request, user, forkedFrom);
        if (request.thumbnailFileId() != null) {
            recipe.updateThumbnail(takeThumbnail(request.thumbnailFileId(), userId));
        }
        Recipe savedRecipe = recipeRepository.save(recipe);

        return RecipeRegisterResponse.from(savedRecipe);
    }
    /**
     * 대표 이미지로 쓸 임시 업로드 파일을 검증하고 영구 보관으로 바꾼다 (데이터셋 첨부 파일과 같은 방식).
     * - 없는 파일 404, 남이 올린 파일은 도용 방지를 위해 거부(FILE_READ_FAILED), 이미지가 아니면 400(RECIPE_THUMBNAIL_NOT_IMAGE)
     *
     * @return 대표 이미지의 S3 key
     */
    private String takeThumbnail(Long fileId, Long userId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!file.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FILE_READ_FAILED);
        }
        if (file.getFileType() == null || !file.getFileType().toLowerCase().startsWith("image/")) {
            throw new BusinessException(ErrorCode.RECIPE_THUMBNAIL_NOT_IMAGE);
        }
        file.markAsPermanent();
        return file.getS3Key();
    }

    /**
     * 내 자산 레시피 목록 조회.
     * 공개 여부와 무관하게 본인이 작성/포크한 레시피를 type(MINE/FORKED)으로 필터링해 반환한다.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<MyRecipeListResponse> getMyRecipes(RecipeListType type, Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Recipe> recipePage = switch (type) {
            case MINE -> recipeRepository.findAllByForkedFromIsNullAndUser_UserIdAndDeletedAtIsNull(userId, pageable);
            case FORKED -> recipeRepository.findAllByForkedFromIsNotNullAndUser_UserIdAndDeletedAtIsNull(userId, pageable);
        };

        List<Long> ids = recipePage.getContent().stream().map(Recipe::getRecipeId).toList();
        Map<Long, InspectionSummary> verificationMap =
                inspectionResultService.getSummaries(InspectionTargetType.RECIPE, ids);
        Map<Long, InspectionTargetResponse> targetMap =
                inspectionResultService.getRecipeTargets(userId, recipePage.getContent());

        return recipePage.map(recipe -> MyRecipeListResponse.from(
                recipe,
                verificationMap.getOrDefault(recipe.getRecipeId(), InspectionSummary.none()),
                targetMap.get(recipe.getRecipeId())
        ));

    }

    /**
     * 커뮤니티 레시피 목록 조회.
     * sort 값에 따라 정렬 기준을 결정하고, 전문가 검증 요약 정보를 배치로 조회해 N+1을 방지한다.
     * 실제 공개 필터링(isPublic = true)은 RecipeRepository#searchRecipes에서 처리한다.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<RecipeListResponse> getRecipeList(String sort, String keyword, int page, int size) {
        Sort sortOption = switch (sort) {
            case "MOST_POPULAR" -> Sort.by(Sort.Direction.DESC, "likeCount");
            case "MOST_FORKED" -> Sort.by(Sort.Direction.DESC, "forkCount");
            default -> Sort.by(Sort.Direction.DESC, "createdAt"); // LATEST
        };

        Pageable pageable = PageRequest.of(page, size, sortOption);
        Page<Recipe> recipePage = recipeRepository.searchRecipes(keyword, pageable);

        List<Long> ids = recipePage.getContent().stream().map(Recipe::getRecipeId).toList();
        Map<Long, InspectionSummary> verificationMap =
                inspectionResultService.getSummaries(InspectionTargetType.RECIPE, ids);

        return recipePage.map(recipe -> RecipeListResponse.from(
                recipe,
                verificationMap.getOrDefault(recipe.getRecipeId(), InspectionSummary.none())
        ));
    }

    @Override
    @Transactional
    public void deleteRecipe(Long recipeId, Long userId) {
        Recipe recipe = recipeRepository.findByRecipeIdAndDeletedAtIsNull(recipeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!recipe.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_RECIPE_ACCESS);
        }
        // 전문가가 시작하기 전인 검수 신청은 자동 취소·환불하고, 이미 시작됐으면 삭제를 막는다(409)
        inspectionService.cancelForTargetDeletion(userId, InspectionTargetType.RECIPE, recipeId);
        recipe.delete();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Recipe> getPublicRecipesByUser(Long userId) {
        return recipeRepository.findPublicByUserId(userId);
    }

    @Override
    public RecipeRegisterResponse updateRecipe(Long recipeId, RecipeRegisterRequest request, Long userId) {
        Recipe recipe = recipeRepository.findByRecipeIdAndDeletedAtIsNull(recipeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!recipe.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_RECIPE_ACCESS);
        }
        inspectionService.validateTargetEditable(userId, InspectionTargetType.RECIPE, recipeId);
        if (request.thumbnailFileId() != null) {
            recipe.updateThumbnail(takeThumbnail(request.thumbnailFileId(), userId));
        }

        recipe.update(
                request.title(),
                request.description(),
                request.overview(),
                request.features(),
                request.recommendations(),
                request.isPublic(),
                request.modelName(),
                request.steps(),
                request.sampler(),
                request.cfgScale() != null ? BigDecimal.valueOf(request.cfgScale()) : null,
                request.seed() != null ? String.valueOf(request.seed()) : null,
                request.resolution(),
                request.batchSize()
        );

        return RecipeRegisterResponse.from(recipe);
    }
}
