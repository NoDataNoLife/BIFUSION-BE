package com.nodatanolife.bifusion_be.domain.project.dto.response;

import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectMember;
import com.nodatanolife.bifusion_be.domain.project.entity.Project;

import java.util.List;

public record ProjectCreateResponse(
        Long projectId,
        String title,
        List<MemberResponse> members
) {
    public record MemberResponse(
            Long userId,
            String nickname,
            @PresignedUrl String profileImageUrl,
            String role
    ) {
        public static MemberResponse from(ProjectMember member) {
            return new MemberResponse(
                    member.getUser().getUserId(),
                    member.getUser().getNickname(),
                    member.getUser().getProfileImageUrl(),
                    member.getRole().name()
            );
        }
    }

    public static ProjectCreateResponse of(Project project, List<MemberResponse> members) {
        return new ProjectCreateResponse(
                project.getProjectId(),
                project.getTitle(),
                members
        );
    }
}