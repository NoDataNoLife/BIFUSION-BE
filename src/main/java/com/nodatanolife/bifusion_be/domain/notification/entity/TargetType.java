package com.nodatanolife.bifusion_be.domain.notification.entity;

public enum TargetType {
    PROJECT,
    // 프로젝트 초대 알림. targetId는 ProjectInvitation ID
    INVITATION,
    JOB,
    RECIPE,
    DATASET,
    QNA,
    RECRUITMENT,
    EXPERT
}
