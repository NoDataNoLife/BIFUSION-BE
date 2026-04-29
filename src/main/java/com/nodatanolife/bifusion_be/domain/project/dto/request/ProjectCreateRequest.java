package com.nodatanolife.bifusion_be.domain.project.dto.request;

import com.nodatanolife.bifusion_be.domain.project.entity.ProjectRole;
import java.util.List;

public record ProjectCreateRequest(
        String title,
        String description,
        boolean isPublic,
        List<MemberRequest> members
) {
        public record MemberRequest(
                Long userId,
                ProjectRole role
        ) {}
}
