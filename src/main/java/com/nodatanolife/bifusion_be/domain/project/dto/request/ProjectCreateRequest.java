package com.nodatanolife.bifusion_be.domain.project.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 프로젝트 생성
 */
public record ProjectCreateRequest(
        @NotBlank(message = "프로젝트 제목은 필수입니다.")
        String title,

        String description,

        boolean isPublic
) {
}