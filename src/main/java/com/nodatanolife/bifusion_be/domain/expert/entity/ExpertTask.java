package com.nodatanolife.bifusion_be.domain.expert.entity;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "expert_task")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpertTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expert_task_id")
    private Long expertTaskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expert_id", nullable = false)
    private User expert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expert_verification_id", nullable = false)
    private ExpertVerification expertVerification;

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

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "review_code", length = 20)
    private String reviewCode;

    @Column(name = "feedback")
    private String feedback;

    @Column(name = "draft_feedback")
    private String draftFeedback;
}
