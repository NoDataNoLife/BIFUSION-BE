package com.nodatanolife.bifusion_be.domain.project.dto.response;

import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
import com.nodatanolife.bifusion_be.domain.project.entity.Project;

public record ProjectUpdateResponse(
        Long projectId,
        String title,
        String description,
        @PresignedUrl String bannerImageUrl
) {
    public static ProjectUpdateResponse of(Project project) {
        return new ProjectUpdateResponse(
                project.getProjectId(),
                project.getTitle(),
                project.getDescription(),
                project.getBannerImageUrl()
        );
    }
}