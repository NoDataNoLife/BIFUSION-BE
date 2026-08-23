package com.nodatanolife.bifusion_be.domain.expert.entity;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "expert_task")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpertTask extends BaseEntity {

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

    public void anonymize(User deletedUser) {
        this.expert = deletedUser;
    }

    public void start() {
        this.status = "IN_PROGRESS";
        this.startedAt = LocalDateTime.now();
    }

    public void saveDraftComment(String comment) {
        this.draftFeedback = comment;
    }
}
