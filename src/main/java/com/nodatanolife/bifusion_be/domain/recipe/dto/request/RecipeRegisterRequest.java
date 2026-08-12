package com.nodatanolife.bifusion_be.domain.recipe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;


public record RecipeRegisterRequest(
        @NotBlank String title,
        @NotBlank String description,
        @NotBlank String modelName,
        @NotNull Integer steps,
        @NotBlank String sampler,
        @NotNull Float cfgScale,
        @NotNull Integer seed,
        @NotBlank String resolution,
        @NotNull Integer batchSize,
        String overview,
        List<String> features,
        List<String> recommendations,
        Long forkedFromId
) {
}