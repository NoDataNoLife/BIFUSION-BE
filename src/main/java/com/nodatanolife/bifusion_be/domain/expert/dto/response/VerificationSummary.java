package com.nodatanolife.bifusion_be.domain.expert.dto.response;

import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertVerification;
import com.nodatanolife.bifusion_be.domain.expert.entity.VerificationStatus;

/**
 * Recipe/Dataset 상세 응답이 노출하는 검증 상태 + 결과 묶음.
 * ExpertVerification.status를 조회 시점에 파생시켜 만들며, Recipe/Dataset에는 별도로 저장하지 않는다.
 */
public record VerificationSummary(
        VerificationStatus status,
        VerificationResultResponse result
) {
    private static final VerificationSummary NONE = new VerificationSummary(VerificationStatus.NONE, null);

    public static VerificationSummary none() {
        return NONE;
    }

    public static VerificationSummary of(ExpertVerification verification) {
        return switch (verification.getStatus()) {
            case PENDING -> new VerificationSummary(VerificationStatus.PENDING, null);
            case APPROVED -> new VerificationSummary(VerificationStatus.COMPLETED, VerificationResultResponse.from(verification));
            case REJECTED -> NONE;
        };
    }
}
