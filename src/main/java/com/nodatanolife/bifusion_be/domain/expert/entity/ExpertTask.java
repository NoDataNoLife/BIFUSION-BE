package com.nodatanolife.bifusion_be.domain.expert.entity;

import com.nodatanolife.bifusion_be.domain.expert.entity.converter.InspectionRequestStatusConverter;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "expert_task")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpertTask extends BaseEntity {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expert_task_id")
    private Long expertTaskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expert_id", nullable = false)
    private User expert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_request_id", nullable = false)
    private InspectionRequest inspectionRequest;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "reward")
    private Integer reward;

    @Column(name = "satisfaction")
    private BigDecimal satisfaction;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "review_code", length = 20)
    private String reviewCode;

    @Column(name = "feedback")
    private String feedback;

    @Column(name = "draft_feedback")
    private String draftFeedback;

    // 이 회차의 판정 (APPROVED/REJECTED). 신청(InspectionRequest)은 재신청 때 다시 쓰이므로 회차 결과는 task에 남긴다. 완료 전 null
    @Convert(converter = InspectionRequestStatusConverter.class)
    @Column(name = "result", length = 20)
    private InspectionRequestStatus result;

    @Builder
    private ExpertTask(User expert, InspectionRequest inspectionRequest, Integer reward) {
        this.expert = expert;
        this.inspectionRequest = inspectionRequest;
        this.reward = reward;
        this.status = STATUS_PENDING;
    }

    /**
     * 풀에 올라온 검수 신청을 전문가가 시작해 맡는다. 맡는 즉시 검수 중(IN_PROGRESS)이 된다.
     * 보상은 신청 포인트와 같으며, 승인·반려 시 수수료를 뗀 금액이 지급된다.
     * reviewCode는 ID가 필요해 저장 후 assignReviewCode()로 채운다.
     */
    public static ExpertTask claim(User expert, InspectionRequest inspectionRequest) {
        ExpertTask task = ExpertTask.builder()
                .expert(expert)
                .inspectionRequest(inspectionRequest)
                .reward(inspectionRequest.getRewardPoints())
                .build();
        task.start();
        return task;
    }

    /**
     * 화면 표시용 검수 코드 (예: REV-023). 저장으로 ID가 생긴 뒤 호출한다.
     */
    public void assignReviewCode() {
        this.reviewCode = "REV-%03d".formatted(expertTaskId);
    }

    public boolean isCompleted() {
        return STATUS_COMPLETED.equals(status);
    }

    /**
     * 완료된 task로는 검수를 바꿀 수 없다. 반려·취소 후 재신청되면 신청(InspectionRequest)은 다시 PENDING이 되지만
     * 이전 회차 task는 COMPLETED로 남으므로, 이전 task로 새 회차를 승인·반려하지 못하게 막는다.
     */
    public void validateNotCompleted() {
        if (STATUS_COMPLETED.equals(status)) {
            throw new BusinessException(ErrorCode.INSPECTION_ALREADY_PROCESSED);
        }
    }

    /**
     * 이 회차의 판정 (APPROVED/REJECTED). 완료 전이면 null.
     * result 컬럼 도입 전에 끝난 회차는 신청 상태로 대신한다 (docs의 백필 SQL을 실행하면 쓰이지 않는다).
     */
    public InspectionRequestStatus reviewResult() {
        if (!STATUS_COMPLETED.equals(status)) {
            return null;
        }
        return result != null ? result : inspectionRequest.getStatus();
    }

    public void anonymize(User deletedUser) {
        this.expert = deletedUser;
    }

    private void start() {
        this.status = STATUS_IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    public void saveDraftComment(String comment) {
        this.draftFeedback = comment;
    }

    /**
     * 검수 최종 승인/거절 시 호출. 판정과 최종 코멘트를 이 회차에 확정하고,
     * 더 이상 유효하지 않은 임시 저장값(draftFeedback)은 비운다.
     */
    public void complete(InspectionRequestStatus result, String finalComment) {
        this.status = STATUS_COMPLETED;
        this.result = result;
        this.completedAt = LocalDateTime.now();
        this.feedback = finalComment;
        this.draftFeedback = null;
    }
}
