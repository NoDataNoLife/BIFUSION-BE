package com.nodatanolife.bifusion_be.domain.expert.service;

import com.nodatanolife.bifusion_be.domain.expert.dto.response.VerificationSummary;
import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertVerification;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;
import com.nodatanolife.bifusion_be.domain.expert.repository.ExpertVerificationRepository;
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
public class VerificationResultServiceImpl implements VerificationResultService {

    private final ExpertVerificationRepository expertVerificationRepository;

    @Override
    public VerificationSummary getSummary(InspectionTargetType targetType, Long targetId) {
        return expertVerificationRepository
                .findFirstByTargetTypeAndTargetIdOrderByAppliedAtDesc(targetType, targetId)
                .map(VerificationSummary::of)
                .orElseGet(VerificationSummary::none);
    }

    @Override
    public Map<Long, VerificationSummary> getSummaries(InspectionTargetType targetType, List<Long> targetIds) {
        if (targetIds.isEmpty()) {
            return Map.of();
        }

        return expertVerificationRepository
                .findAllByTargetTypeAndTargetIdIn(targetType, targetIds)
                .stream()
                .collect(Collectors.groupingBy(
                        ExpertVerification::getTargetId,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparing(ExpertVerification::getAppliedAt)),
                                latest -> latest.map(VerificationSummary::of).orElseGet(VerificationSummary::none)
                        )
                ));
    }
}
