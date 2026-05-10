package com.nodatanolife.bifusion_be.domain.user.dto.response;

import com.nodatanolife.bifusion_be.domain.user.entity.ActivityType;

import java.util.List;

public record ActivityVisibilityUpdateResponse(
        List<Item> items
) {
    public record Item(
            ActivityType type,
            Long id,
            Boolean isPublic
    ) {}
}
