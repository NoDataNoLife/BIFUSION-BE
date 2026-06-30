package com.nodatanolife.bifusion_be.domain.project.dto.response;


import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectRole;
import java.time.LocalDateTime;
import java.util.List;
import com.nodatanolife.bifusion_be.domain.project.entity.Project;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectMember;


public record ProjectDetailResponse(
        Long projectId,
        String title,
        String description,
        ProjectRole role,
        @PresignedUrl String bannerImageUrl,
        Boolean isFavorited,
        Integer memberCount,
        List<ProjectMemberResponse> members,
        LocalDateTime lastActivityAt
) {
    public static ProjectDetailResponse from(ProjectMember membership) {
        Project project = membership.getProject();
        List<ProjectMemberResponse> memberResponses = project.getProjectMembers().stream()
                .map(pm -> new ProjectMemberResponse(
                        pm.getUser().getUserId(),
                        pm.getUser().getNickname(),
                        pm.getUser().getProfileImageUrl()
                ))
                .toList();
        return new ProjectDetailResponse(
                project.getProjectId(),
                project.getTitle(),
                project.getDescription(),
                membership.getRole(),
                project.getBannerImageUrl(),
                false,
                memberResponses.size(),
                memberResponses,
                project.getUpdatedAt()
        );
    }
}