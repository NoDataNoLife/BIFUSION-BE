package com.nodatanolife.bifusion_be.domain.expert.service;

import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionSummary;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionTargetResponse;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequest;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequestStatus;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationConfig;
import com.nodatanolife.bifusion_be.domain.expert.repository.InspectionRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InspectionResultServiceImpl implements InspectionResultService {

    private final InspectionRequestRepository inspectionRequestRepository;

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

    /**
     * 대상별로 신청을 한 번에 조회한다 (N+1 방지).
     * - status·result: 가장 최근 신청 기준 (getSummaries와 같은 기준이지만 반려도 그대로 보여준다)
     * - canRequest: 본인 소유이고, 본인의 기존 신청이 없거나 반려됐을 때 true.
     *   검수 신청 API(InspectionServiceImpl.createInspection)의 소유자·중복 검증과 같은 기준이며, 포인트는 신청 시 검증한다.
     */
    @Override
    public Map<Long, InspectionTargetResponse> getAugmentedDataTargets(Long userId, List<AugmentationConfig> configs) {
        if (configs.isEmpty()) {
            return Map.of();
        }
        List<Long> configIds = configs.stream().map(AugmentationConfig::getAugmentationConfigId).toList();
        Map<Long, List<InspectionRequest>> requestsByTargetId = inspectionRequestRepository
                .findAllByTargetTypeAndTargetIdIn(InspectionTargetType.AUGMENTED_DATA, configIds)
                .stream()
                .collect(Collectors.groupingBy(InspectionRequest::getTargetId));

        return configs.stream().collect(Collectors.toMap(
                AugmentationConfig::getAugmentationConfigId,
                config -> {
                    List<InspectionRequest> requests = requestsByTargetId.getOrDefault(
                            config.getAugmentationConfigId(), List.of());
                    InspectionRequest latest = requests.stream()
                            .max(Comparator.comparing(InspectionRequest::getAppliedAt,
                                    Comparator.nullsLast(Comparator.naturalOrder())))
                            .orElse(null);
                    boolean hasActiveRequest = requests.stream()
                            .anyMatch(request -> request.getUser().getUserId().equals(userId)
                                    && request.getStatus() != InspectionRequestStatus.REJECTED);
                    boolean canRequest = config.getJob().isOwnedBy(userId) && !hasActiveRequest;
                    return InspectionTargetResponse.of(InspectionTargetType.AUGMENTED_DATA,
                            config.getAugmentationConfigId(), canRequest, latest);
                }
        ));
    }
}
