package com.nodatanolife.bifusion_be.domain.project.dto.request;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectRole;


public record MemberRoleUpdateRequest(
        ProjectRole role
) {}