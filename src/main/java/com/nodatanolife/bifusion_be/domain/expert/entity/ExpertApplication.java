package com.nodatanolife.bifusion_be.domain.expert.entity;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "expert_application")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpertApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expert_application_id")
    private Long expertApplicationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    // 관리자 user_id (nullable)
    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "application_document_url")
    private String applicationDocumentUrl;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
