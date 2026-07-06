package com.nodatanolife.bifusion_be.domain.expert.entity;

/**
 * 전문가 인증 신청의 심사 상태.
 * - NONE: 신청 내역 없음 (기본값)
 * - PENDING: 심사 대기 중
 * - APPROVED: 승인됨(전문가)
 * - REJECTED: 반려됨
 */
public enum ExpertApplicationStatus {
    NONE,
    PENDING,
    APPROVED,
    REJECTED
}
