package com.nodatanolife.bifusion_be.domain.expert.service;

import com.nodatanolife.bifusion_be.domain.dataset.entity.Dataset;
import com.nodatanolife.bifusion_be.domain.dataset.repository.DatasetRepository;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequest;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationConfig;
import com.nodatanolife.bifusion_be.domain.job.repository.AugmentationConfigRepository;
import com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe;
import com.nodatanolife.bifusion_be.domain.recipe.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 검수 목록(전문가·신청자)에 보여줄 대상 이름·대표 이미지를 종류별로 한 번씩 일괄 조회한다 (N+1 방지).
 * - AUGMENTED_DATA: 프로젝트명 + ML 미리보기 이미지(AugmentationConfig.previewKey)
 * - RECIPE: 제목 + 썸네일
 * - UPLOADED_DATA: 제목 + 배너
 * 완료된 검수 이력을 보여주기 위해 삭제된 대상도 찾는다 (deleted=true).
 * Job·Project를 지연 로딩하므로 트랜잭션 안에서 호출한다.
 */
@Component
@RequiredArgsConstructor
public class InspectionTargetDisplayResolver {

    private final AugmentationConfigRepository augmentationConfigRepository;
    private final RecipeRepository recipeRepository;
    private final DatasetRepository datasetRepository;

    /**
     * @param jobId 증강 데이터 화면 이동용. 레시피·데이터셋은 null
     */
    public record TargetDisplay(Long jobId, String name, String imageUrl, boolean deleted) {
    }

    /**
     * 조회 결과. 대상 row가 없거나 targetType이 비어 있는 과거 신청은 get이 null을 돌려준다.
     */
    public static final class TargetDisplays {
        private final Map<InspectionTargetType, Map<Long, TargetDisplay>> byType;

        private TargetDisplays(Map<InspectionTargetType, Map<Long, TargetDisplay>> byType) {
            this.byType = byType;
        }

        public TargetDisplay get(InspectionRequest request) {
            InspectionTargetType targetType = request.getTargetType();
            return targetType == null ? null : byType.get(targetType).get(request.getTargetId());
        }
    }

    public TargetDisplays resolve(Collection<InspectionRequest> requests) {
        return new TargetDisplays(Map.of(
                InspectionTargetType.AUGMENTED_DATA, augmentationConfigRepository
                        .findAllById(targetIdsOf(requests, InspectionTargetType.AUGMENTED_DATA)).stream()
                        .filter(c -> c.getJob() != null && c.getJob().getProject() != null)
                        .collect(Collectors.toMap(AugmentationConfig::getAugmentationConfigId,
                                c -> new TargetDisplay(c.getJob().getJobId(), c.getJob().getProject().getTitle(),
                                        c.getPreviewKey(), c.isDeleted()))),
                InspectionTargetType.RECIPE, recipeRepository
                        .findAllById(targetIdsOf(requests, InspectionTargetType.RECIPE)).stream()
                        .collect(Collectors.toMap(Recipe::getRecipeId,
                                r -> new TargetDisplay(null, r.getTitle(), r.getThumbnailUrl(), r.isDeleted()))),
                InspectionTargetType.UPLOADED_DATA, datasetRepository
                        .findAllById(targetIdsOf(requests, InspectionTargetType.UPLOADED_DATA)).stream()
                        .collect(Collectors.toMap(Dataset::getDatasetId,
                                d -> new TargetDisplay(null, d.getTitle(), d.getBannerImageUrl(), d.isDeleted())))
        ));
    }

    private static List<Long> targetIdsOf(Collection<InspectionRequest> requests, InspectionTargetType targetType) {
        return requests.stream()
                .filter(r -> r.getTargetType() == targetType)
                .map(InspectionRequest::getTargetId)
                .distinct()
                .toList();
    }
}
