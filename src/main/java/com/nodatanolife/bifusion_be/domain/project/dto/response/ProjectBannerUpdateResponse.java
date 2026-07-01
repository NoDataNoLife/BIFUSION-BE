package com.nodatanolife.bifusion_be.domain.project.dto.response;

import com.nodatanolife.bifusion_be.domain.project.entity.Project;

public record ProjectBannerUpdateResponse(
        Long projectId,
        String bannerImageUrl
) {
    public static ProjectBannerUpdateResponse of(Project project) {
        return new ProjectBannerUpdateResponse(
                project.getProjectId(),
                project.getBannerImageUrl()
        );
    }
}