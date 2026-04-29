package com.nodatanolife.bifusion_be.domain.project.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProjectRole {
    MANAGER("ROLE_MANAGER", "관리자"),
    MEMBER("ROLE_MEMBER", "멤버");

    private final String key;
    private final String title;
}