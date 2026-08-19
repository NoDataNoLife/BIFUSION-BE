package com.nodatanolife.bifusion_be.domain.expert.entity;

/**
 * Recipe/Dataset 등 게시글이 노출하는 검증 진행 상태.
 * InspectionRequestStatus(PENDING/APPROVED/REJECTED)와 별개로,
 * 게시글 입장에서는 검증 이력이 없거나(NONE) 신청 중이거나(PENDING) 완료된(COMPLETED) 3가지로만 구분한다.
 */
public enum InspectionStatus {
    NONE,
    PENDING,
    COMPLETED
}
