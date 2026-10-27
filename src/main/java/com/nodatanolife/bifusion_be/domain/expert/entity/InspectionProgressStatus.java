package com.nodatanolife.bifusion_be.domain.expert.entity;

/**
 * 신청자 화면("내가 요청한 검수")에 보여주는 진행 상태.
 * 신청 상태(InspectionRequestStatus)와 현재 회차 task 상태(ExpertTask.status)를 합쳐 만든다.
 * - PENDING: 신청됐고 전문가가 아직 시작하지 않음 (전문가 풀에서 대기 중)
 * - IN_PROGRESS: 전문가가 검수를 시작함
 * - APPROVED / REJECTED: 승인·반려로 확정됨
 * 취소된(CANCELLED) 신청은 신청자 목록에 나오지 않으므로 대응하는 값이 없다.
 */
public enum InspectionProgressStatus {
    PENDING,
    IN_PROGRESS,
    APPROVED,
    REJECTED;

    /**
     * @param taskStatus 현재 회차 task 상태. 아직 아무도 시작하지 않았으면 null
     * @return 취소된 신청이면 null
     */
    public static InspectionProgressStatus of(InspectionRequestStatus requestStatus, String taskStatus) {
        return switch (requestStatus) {
            case APPROVED -> APPROVED;
            case REJECTED -> REJECTED;
            case CANCELLED -> null;
            case PENDING -> ExpertTask.STATUS_IN_PROGRESS.equals(taskStatus) ? IN_PROGRESS : PENDING;
        };
    }
}
