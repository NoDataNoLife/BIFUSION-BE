package com.nodatanolife.bifusion_be.domain.expert.service;

import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionSummary;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequest;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;
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
                                Collectors.maxBy(Comparator.comparing(InspectionRequest::getAppliedAt)),
                                latest -> latest.map(InspectionSummary::of).orElseGet(InspectionSummary::none)
                        )
                ));
    }
}
