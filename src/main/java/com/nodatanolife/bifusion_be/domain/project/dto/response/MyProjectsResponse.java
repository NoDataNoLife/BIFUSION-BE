package com.nodatanolife.bifusion_be.domain.project.dto.response;

import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectMember;

import java.util.List;

public record MyProjectsResponse(
        List<Item> items,
        String nextCursor,
        boolean hasNext,
        long totalCount
) {
    public record Item(
            Long projectId,
            String title,
            @PresignedUrl String bannerImageUrl,
            Boolean isPublic,
            String role
    ) {}

    public static MyProjectsResponse of(
            List<ProjectMember> members,
            String nextCursor,
            boolean hasNext,
            long totalCount
    ) {
        List<Item> items = members.stream()
                .map(m -> new Item(
                        m.getProject().getProjectId(),
                        m.getProject().getTitle(),
                        m.getProject().getBannerImageUrl(),
                        m.getIsPublic(),
                        m.getRole().name()
                ))
                .toList();

        return new MyProjectsResponse(items, nextCursor, hasNext, totalCount);
    }
}
