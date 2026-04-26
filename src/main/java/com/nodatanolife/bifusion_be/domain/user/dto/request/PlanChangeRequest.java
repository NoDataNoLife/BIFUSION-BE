package com.nodatanolife.bifusion_be.domain.user.dto.request;

import com.nodatanolife.bifusion_be.domain.user.entity.SubscriptionTier;

public record PlanChangeRequest(SubscriptionTier planType) {
}