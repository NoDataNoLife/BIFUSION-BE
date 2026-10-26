package com.nodatanolife.bifusion_be.domain.expert.service;

import com.nodatanolife.bifusion_be.domain.dataset.entity.Dataset;
import com.nodatanolife.bifusion_be.domain.dataset.entity.DatasetType;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionSummary;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionTargetResponse;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequest;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequestStatus;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;
import com.nodatanolife.bifusion_be.domain.file.entity.File;
import com.nodatanolife.bifusion_be.domain.file.repository.FileRepository;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationConfig;
import com.nodatanolife.bifusion_be.domain.expert.repository.InspectionRequestRepository;
import com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe;
import com.nodatanolife.bifusion_be.domain.recipe.repository.RecipeForkHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InspectionResultServiceImpl implements InspectionResultService {

    private final InspectionRequestRepository inspectionRequestRepository;
    private final RecipeForkHistoryRepository recipeForkHistoryRepository;
    private final InspectionRewardPolicy inspectionRewardPolicy;
    private final FileRepository fileRepository;

    @Override
    public InspectionSummary getSummary(InspectionTargetType targetType, Long targetId) {
        return inspectionRequestRepository
                .findFirstByTargetTypeAndTargetIdOrderByAppliedAtDesc(targetType, targetId)
                .map(InspectionSummary::of)
                .orElseGet(InspectionSummary::none);
    }

    @Override
    public Map<Long, InspectionSummary> getSummaries(InspectionTargetType targetType, List<Long> targetIds) {
        if (targetIds.isEmpty()) {
            return Map.of();
        }

        return inspectionRequestRepository
                .findAllByTargetTypeAndTargetIdIn(targetType, targetIds)
                .stream()
                .collect(Collectors.groupingBy(
                        InspectionRequest::getTargetId,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparing(
                                        InspectionRequest::getAppliedAt,
                                        Comparator.nullsLast(Comparator.naturalOrder()))),
                                latest -> latest.map(InspectionSummary::of).orElseGet(InspectionSummary::none)
                        )
                ));
    }

    @Override
    public Map<Long, InspectionTargetResponse> getAugmentedDataTargets(Long userId, List<AugmentationConfig> configs) {
        // 리워드 범위의 기준인 증강 결과 파일 크기를 한 번에 조회한다
        List<String> datasetKeys = configs.stream().map(AugmentationConfig::getDatasetKey).filter(Objects::nonNull).toList();
        Map<String, Long> sizeByKey = datasetKeys.isEmpty() ? Map.of()
                : fileRepository.findAllByS3KeyIn(datasetKeys).stream()
                .filter(file -> file.getFileSize() != null)
                .collect(Collectors.toMap(File::getS3Key, File::getFileSize, (existing, replacement) -> existing));
        return getTargets(userId, InspectionTargetType.AUGMENTED_DATA, configs,
                AugmentationConfig::getAugmentationConfigId,
                config -> config.getJob().isOwnedBy(userId),
                config -> inspectionRewardPolicy.forSize(
                        config.getDatasetKey() != null ? sizeByKey.get(config.getDatasetKey()) : null));
    }

    /**
     * 원본을 그대로 복제한 사본·삭제된 레시피는 신청할 수 없다 (InspectionServiceImpl.validateTargetOwnership과 같은 기준).
     * 원본 내용을 불러와 다시 등록한 레시피는 forkedFrom이 있어도 신청할 수 있다.
     */
    @Override
    public Map<Long, InspectionTargetResponse> getRecipeTargets(Long userId, List<Recipe> recipes) {
        if (recipes.isEmpty()) {
            return Map.of();
        }
        Set<Long> copiedRecipeIds = Set.copyOf(recipeForkHistoryRepository
                .findForkedRecipeIdsIn(recipes.stream().map(Recipe::getRecipeId).toList()));
        return getTargets(userId, InspectionTargetType.RECIPE, recipes,
                Recipe::getRecipeId,
                recipe -> recipe.getUser().getUserId().equals(userId)
                        && !copiedRecipeIds.contains(recipe.getRecipeId())
                        && !recipe.isDeleted(),
                recipe -> inspectionRewardPolicy.forRecipe());
    }

    /**
     * 업로드(UPLOADED) 데이터셋만 대상이다. 증강 데이터셋은 AUGMENTED_DATA(AugmentationConfig 기준)로 신청한다.
     */
    @Override
    public Map<Long, InspectionTargetResponse> getUploadedDataTargets(Long userId, List<Dataset> datasets) {
        List<Dataset> uploaded = datasets.stream()
                .filter(dataset -> dataset.getType() == DatasetType.UPLOADED)
                .toList();
        return getTargets(userId, InspectionTargetType.UPLOADED_DATA, uploaded,
                Dataset::getDatasetId,
                dataset -> dataset.getUser().getUserId().equals(userId) && !dataset.isDeleted(),
                inspectionRewardPolicy::forDataset);
    }

    /**
     * 대상별로 신청을 한 번에 조회한다 (N+1 방지).
     * - status·result: 가장 최근 신청 기준 (getSummaries와 같은 기준이지만 반려도 그대로 보여준다)
     * - canRequest: 신청 가능한 대상(eligible)이고, 본인의 기존 신청이 없거나 반려·취소됐을 때 true.
     *   검수 신청 API(InspectionServiceImpl.createInspection)의 소유자·중복 검증과 같은 기준이며, 포인트는 신청 시 검증한다.
     */
    private <T> Map<Long, InspectionTargetResponse> getTargets(Long userId, InspectionTargetType targetType, List<T> targets,
                                                             Function<T, Long> idOf, Predicate<T> eligible,
                                                             Function<T, InspectionRewardPolicy.RewardRange> rewardRangeOf) {
        if (targets.isEmpty()) {
            return Map.of();
        }
        List<Long> targetIds = targets.stream().map(idOf).toList();
        Map<Long, List<InspectionRequest>> requestsByTargetId = inspectionRequestRepository
                .findAllByTargetTypeAndTargetIdIn(targetType, targetIds)
                .stream()
                .collect(Collectors.groupingBy(InspectionRequest::getTargetId));

        return targets.stream().collect(Collectors.toMap(
                idOf,
                target -> {
                    List<InspectionRequest> requests = requestsByTargetId.getOrDefault(idOf.apply(target), List.of());
                    InspectionRequest latest = requests.stream()
                            .max(Comparator.comparing(InspectionRequest::getAppliedAt,
                                    Comparator.nullsLast(Comparator.naturalOrder())))
                            .orElse(null);
                    boolean hasActiveRequest = requests.stream()
                            .anyMatch(request -> request.getUser().getUserId().equals(userId)
                                    && request.getStatus() != InspectionRequestStatus.REJECTED
                                    && request.getStatus() != InspectionRequestStatus.CANCELLED);
                    boolean canRequest = eligible.test(target) && !hasActiveRequest;
                    InspectionRewardPolicy.RewardRange range = rewardRangeOf.apply(target);
                    return InspectionTargetResponse.of(targetType, idOf.apply(target), canRequest, latest,
                            range.min(), range.max());
                }
        ));
    }
}
