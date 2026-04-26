package com.nodatanolife.bifusion_be.domain.user.dto.response;

import com.nodatanolife.bifusion_be.domain.user.entity.SubscriptionTier;

public record PlanChangeResponse(
        SubscriptionTier planType,
        String message
) {
    public static PlanChangeResponse of(SubscriptionTier planType, String message) {
        return new PlanChangeResponse(planType, message);
    }
}