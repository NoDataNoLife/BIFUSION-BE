package com.nodatanolife.bifusion_be.domain.user.service;

import com.nodatanolife.bifusion_be.domain.dataset.repository.DatasetRepository;
import com.nodatanolife.bifusion_be.domain.qna.repository.ExpertQnARepository;
import com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe;
import com.nodatanolife.bifusion_be.domain.recipe.repository.RecipeRepository;
import com.nodatanolife.bifusion_be.domain.recipe.repository.RecipeReviewRepository;
import com.nodatanolife.bifusion_be.domain.recruitment.entity.Recruit;
import com.nodatanolife.bifusion_be.domain.recruitment.repository.RecruitApplicationRepository;
import com.nodatanolife.bifusion_be.domain.recruitment.repository.RecruitRepository;
import com.nodatanolife.bifusion_be.domain.user.dto.response.MyActivityListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyActivityServiceImpl implements MyActivityService {

    private final RecipeRepository recipeRepository;
    private final RecipeReviewRepository recipeReviewRepository;
    private final DatasetRepository datasetRepository;
    private final ExpertQnARepository expertQnARepository;
    private final RecruitRepository recruitRepository;
    private final RecruitApplicationRepository recruitApplicationRepository;

    /**
     * Recipe/Dataset/ExpertQnA/Recruit를 최신순으로 병합 조회한다.
     * 각 하위 쿼리는 (createdAt, PK) 커서 이후 데이터를 size+1개씩 가져오고,
     * 병합·정렬 후 size+1번째 존재 여부로 hasNext를 판단한다 (count 쿼리 불필요).
     */
    @Override
    public MyActivityListResponse getMyActivities(Long userId, String cursor, int size) {
        size = Math.max(1, size);

        boolean hasCursor = cursor != null;
        LocalDateTime cursorCreatedAt = null;
        Long cursorId = null;
        if (hasCursor) {
            ActivityCursor.Value decoded = ActivityCursor.decode(cursor);
            cursorCreatedAt = decoded.createdAt();
            cursorId = decoded.id();
        }

        Pageable limit = PageRequest.of(0, size + 1);
        List<MyActivityListResponse.Item> merged = new ArrayList<>();

        List<Recipe> recipes = recipeRepository.findMyActivities(userId, hasCursor, cursorCreatedAt, cursorId, limit);
        Map<Long, Long> commentCounts = countRecipeComments(recipes);
        recipes.forEach(recipe -> merged.add(MyActivityListResponse.RecipeItem.from(
                recipe,
                commentCounts.getOrDefault(recipe.getRecipeId(), 0L).intValue()
        )));

        datasetRepository.findMyActivities(userId, hasCursor, cursorCreatedAt, cursorId, limit)
                .forEach(dataset -> merged.add(MyActivityListResponse.DatasetItem.from(dataset)));

        expertQnARepository.findMyActivities(userId, hasCursor, cursorCreatedAt, cursorId, limit)
                .forEach(qna -> merged.add(MyActivityListResponse.QnaItem.from(qna)));

        List<Recruit> recruits = recruitRepository.findMyActivities(userId, hasCursor, cursorCreatedAt, cursorId, limit);
        Map<Long, Long> applicationCounts = countApplications(recruits);
        recruits.forEach(recruit -> merged.add(MyActivityListResponse.RecruitItem.from(
                recruit,
                applicationCounts.getOrDefault(recruit.getRecruitId(), 0L).intValue()
        )));

        merged.sort(Comparator.comparing(MyActivityListResponse.Item::createdAt).reversed());

        boolean hasNext = merged.size() > size;
        List<MyActivityListResponse.Item> pageItems = hasNext ? merged.subList(0, size) : merged;

        String nextCursor = null;
        if (hasNext && !pageItems.isEmpty()) {
            MyActivityListResponse.Item last = pageItems.get(pageItems.size() - 1);
            nextCursor = ActivityCursor.encode(last.createdAt(), last.activityId());
        }

        return new MyActivityListResponse(pageItems, hasNext, nextCursor);
    }

    private Map<Long, Long> countRecipeComments(List<Recipe> recipes) {
        if (recipes.isEmpty()) {
            return Map.of();
        }
        List<Long> recipeIds = recipes.stream().map(Recipe::getRecipeId).toList();
        return recipeReviewRepository.countByRecipeIds(recipeIds).stream()
                .collect(Collectors.toMap(
                        RecipeReviewRepository.RecipeReviewCount::getRecipeId,
                        RecipeReviewRepository.RecipeReviewCount::getCount
                ));
    }

    private Map<Long, Long> countApplications(List<Recruit> recruits) {
        if (recruits.isEmpty()) {
            return Map.of();
        }
        List<Long> recruitIds = recruits.stream().map(Recruit::getRecruitId).toList();
        return recruitApplicationRepository.countByRecruitIds(recruitIds).stream()
                .collect(Collectors.toMap(
                        RecruitApplicationRepository.RecruitMemberCount::getRecruitId,
                        RecruitApplicationRepository.RecruitMemberCount::getCount
                ));
    }
}
