package com.nodatanolife.bifusion_be.domain.expert.entity;

public enum InspectionRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    // 신청자가 전문가 검수 시작 전에 취소 (포인트 환불). 같은 대상에 다시 신청할 수 있다.
    CANCELLED
}
