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
    EXPERT,
    // 새 검수 요청 알림(전문가 검수 목록의 검수 전 항목). targetId는 InspectionRequest ID
    INSPECTION_REQUEST
}
