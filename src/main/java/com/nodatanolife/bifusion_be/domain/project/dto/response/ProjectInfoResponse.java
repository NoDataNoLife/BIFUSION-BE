package com.nodatanolife.bifusion_be.domain.project.dto.response;

import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
import com.nodatanolife.bifusion_be.domain.project.entity.Project;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectMember;
import java.util.List;

public record ProjectInfoResponse(
        Long projectId,
        String title,
        String description,
        @PresignedUrl String bannerImageUrl,
        List<MemberResponse> members
) {
    public record MemberResponse(
            Long userId,
            String nickname,
            String email,
            @PresignedUrl String profileImageUrl,
            String role
    ) {}

    public static ProjectInfoResponse of(Project project, List<ProjectMember> members) {
        List<MemberResponse> memberResponses = members.stream()
                .map(m -> new MemberResponse(
                        m.getUser().getUserId(),
                        m.getUser().getNickname(),
                        m.getUser().getEmail(),
                        m.getUser().getProfileImageUrl(),
                        m.getRole().name()
                ))
                .toList();

        return new ProjectInfoResponse(
                project.getProjectId(),
                project.getTitle(),
                project.getDescription(),
                project.getBannerImageUrl(),
                memberResponses
        );
    }
}