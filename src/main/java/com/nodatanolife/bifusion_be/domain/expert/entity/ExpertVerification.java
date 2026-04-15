package com.nodatanolife.bifusion_be.domain.expert.entity;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 전문가 인증 요청 (Recipe, Dataset 등에 대한 검수 요청)
 * target_type: "RECIPE" | "DATASET" 등
 */
@Entity
@Table(name = "expert_verification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpertVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expert_verification_id")
    private Long expertVerificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by", nullable = false)
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
