package com.nodatanolife.bifusion_be.domain.expert.entity;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
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

    @Column(name = "rejection_reason")
    private String rejectionReason;

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
}
