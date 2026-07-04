package com.nodatanolife.bifusion_be.domain.project.dto.response;

import com.nodatanolife.bifusion_be.domain.project.entity.Project;
import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;


public record ProjectBannerUpdateResponse(
        Long projectId,
        @PresignedUrl
        String bannerImageUrl
) {
    public static ProjectBannerUpdateResponse of(Project project) {
        return new ProjectBannerUpdateResponse(
                project.getProjectId(),
                project.getBannerImageUrl()
        );
    }
}