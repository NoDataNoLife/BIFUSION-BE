package com.nodatanolife.bifusion_be.domain.user.entity;

import lombok.Getter;

@Getter
public enum SubscriptionTier {
    BASIC(100),
    PRO(200);

    private final int maxTokens;

    SubscriptionTier(int maxTokens) {
        this.maxTokens = maxTokens;
    }
}