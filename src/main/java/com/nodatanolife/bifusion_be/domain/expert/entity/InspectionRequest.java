package com.nodatanolife.bifusion_be.domain.expert.entity;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 콘텐츠(Recipe, Dataset 등)에 대한 전문가 검수 요청.
 * 사람을 전문가로 인증하는 {@link ExpertApplication}과는 다른 개념이다.
 * target_type: "RECIPE" | "UPLOADED_DATA" | "AUGMENTED_DATA"
 */
@Entity
@Table(
        name = "inspection_request",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_inspection_request_user_target",
                columnNames = {"user_id", "target_type", "target_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InspectionRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inspection_request_id")
    private Long inspectionRequestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "target_id")
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 20)
    private InspectionTargetType targetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private InspectionRequestStatus status;

    @Column(name = "reason")
    private String reason;

    @Column(name = "reward_points")
    private Integer rewardPoints;

    @Column(name = "score")
    private Integer score;

    @Column(name = "comment")
    private String comment;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Builder
    private InspectionRequest(User user, Long targetId, InspectionTargetType targetType, String reason, Integer rewardPoints) {
        this.user = user;
        this.targetId = targetId;
        this.targetType = targetType;
        this.reason = reason;
        this.rewardPoints = rewardPoints;
        this.status = InspectionRequestStatus.PENDING;
        this.appliedAt = LocalDateTime.now();
    }

    public static InspectionRequest create(User user, InspectionTargetType targetType, Long targetId, String reason, Integer rewardPoints) {
        return InspectionRequest.builder()
                .user(user)
                .targetType(targetType)
                .targetId(targetId)
                .reason(reason)
                .rewardPoints(rewardPoints)
                .build();
    }

    public void anonymize(User deletedUser) {
        this.user = deletedUser;
    }

    public void anonymizeReviewer(User deletedUser) {
        this.reviewedBy = deletedUser;
    }

    /**
     * 검수 최종 승인. PENDING 상태에서만 가능하며, 이미 처리된 건은 예외.
     */
    public void approve(User reviewer, String comment) {
        validateNotProcessed();
        this.status = InspectionRequestStatus.APPROVED;
        this.comment = comment;
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
    }

    /**
     * 검수 최종 거절. PENDING 상태에서만 가능하며, 이미 처리된 건은 예외.
     */
    public void reject(User reviewer, String comment) {
        validateNotProcessed();
        this.status = InspectionRequestStatus.REJECTED;
        this.comment = comment;
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
    }

    /**
     * 이미 승인/거절로 확정된 검수 건에 대한 후속 변경(이미지 코멘트 등)을 막을 때 사용.
     * approve/reject 자체의 상태 전이 가드도 겸한다.
     */
    public void validateNotProcessed() {
        if (this.status != InspectionRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.INSPECTION_ALREADY_PROCESSED);
        }
    }

    /**
     * REJECTED로 종료된 검수 요청을 재사용해 PENDING으로 되돌린다.
     * (user, target_type, target_id) 조합에 DB unique 제약이 걸려있어 새 row를 insert할 수 없으므로,
     * 거절된 기존 row를 재신청 시점의 값으로 초기화해 재사용한다.
     * 이전 거절 이력(사유/거절일시)은 보존하지 않는다 — 재신청 시 그대로 덮어써 사라진다.
     * PENDING/APPROVED 상태에서는 이미 유효한 신청이 있는 것이므로 예외.
     */
    public void reapply(String reason, Integer rewardPoints) {
        if (this.status != InspectionRequestStatus.REJECTED) {
            throw new BusinessException(ErrorCode.INSPECTION_ALREADY_EXISTS);
        }
        this.status = InspectionRequestStatus.PENDING;
        this.reason = reason;
        this.rewardPoints = rewardPoints;
        this.score = null;
        this.comment = null;
        this.reviewedBy = null;
        this.reviewedAt = null;
        this.appliedAt = LocalDateTime.now();
    }
}
