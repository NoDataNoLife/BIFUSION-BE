package com.nodatanolife.bifusion_be.domain.project.dto.response;

import com.nodatanolife.bifusion_be.domain.project.entity.ProjectMember;

public record MemberRoleUpdateResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        String role
) {
    public static MemberRoleUpdateResponse of(ProjectMember member) {
        return new MemberRoleUpdateResponse(
                member.getUser().getUserId(),
                member.getUser().getNickname(),
                member.getUser().getProfileImageUrl(),
                member.getRole().name()
        );
    }
}