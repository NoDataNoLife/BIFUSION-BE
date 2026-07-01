package com.nodatanolife.bifusion_be.domain.project.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ProjectUpdateRequest(
        @NotBlank(message = "프로젝트 제목은 필수입니다.")
        String title,
        String description
) {}