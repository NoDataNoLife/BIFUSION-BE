package com.nodatanolife.bifusion_be.domain.user.dto.request;

import com.nodatanolife.bifusion_be.domain.user.entity.ActivityType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ActivityVisibilityUpdateRequest(
        @NotEmpty
        @Valid
        List<Item> items
) {
    public record Item(
            @NotNull ActivityType type,
            @NotNull Long id,
            @NotNull Boolean isPublic
    ) {}
}
