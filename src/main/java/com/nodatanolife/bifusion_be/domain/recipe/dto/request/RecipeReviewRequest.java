package com.nodatanolife.bifusion_be.domain.recipe.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecipeReviewRequest(
        @NotNull @Min(1) @Max(5)
        Integer rating,

        @NotBlank
        String content
) {}
