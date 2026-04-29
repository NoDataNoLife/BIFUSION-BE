package com.nodatanolife.bifusion_be.domain.project.dto.request;

/**
 * 프로젝트 생성
 */
public record ProjectCreateRequest(
        String title,
        String description,
        boolean isPublic
) {
}
