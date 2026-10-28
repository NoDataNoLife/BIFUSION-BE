package com.nodatanolife.bifusion_be.domain.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectUpdateRequest(
        @NotBlank(message = "프로젝트 제목은 필수입니다.")
        @Size(max = 100, message = "프로젝트 제목은 100자 이하로 입력해 주세요.")
        String title,
        @Size(max = 2000, message = "프로젝트 설명은 2000자 이하로 입력해 주세요.")
        String description
) {}