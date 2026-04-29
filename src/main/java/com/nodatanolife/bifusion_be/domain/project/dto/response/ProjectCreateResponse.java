package com.nodatanolife.bifusion_be.domain.project.dto.response;

import com.nodatanolife.bifusion_be.domain.project.entity.Project;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectRole;
import java.util.List;

public record ProjectCreateResponse(
        Long projectId,
        String title,
        List<MemberResponse> members
) {
    public record MemberResponse(
            Long userId,
            String nickname,
            String profileImageUrl,
            ProjectRole role
    ) {}

    public static ProjectCreateResponse of(Project project, List<MemberResponse> members) {
        return new ProjectCreateResponse(
                project.getProjectId(),
                project.getTitle(),
                members
        );
    }
}