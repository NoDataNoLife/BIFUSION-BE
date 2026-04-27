package com.nodatanolife.bifusion_be.domain.project.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ProjectVisibilityUpdateRequest(
        @NotEmpty @Valid List<Item> items
) {
    public record Item(
            @NotNull Long projectId,
            @NotNull Boolean isPublic
    ) {}
}
