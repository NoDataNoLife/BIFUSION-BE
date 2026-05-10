package com.nodatanolife.bifusion_be.domain.project.dto.response;

import com.nodatanolife.bifusion_be.domain.project.entity.ProjectMember;

import java.util.List;

public record MyProjectsResponse(
        List<Item> items
) {
    public record Item(
            Long projectId,
            String title,
            String bannerImageUrl,
            Boolean isPublic,
            String role
    ) {}

    public static MyProjectsResponse from(List<ProjectMember> members) {
        List<Item> items = members.stream()
                .map(m -> new Item(
                        m.getProject().getProjectId(),
                        m.getProject().getTitle(),
                        m.getProject().getBannerImageUrl(),
                        m.getProject().getIsPublic(),
                        m.getRole().name()
                ))
                .toList();

        return new MyProjectsResponse(items);
    }
}
