package com.nodatanolife.bifusion_be.domain.expert.entity;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "expert_application")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpertApplication extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expert_application_id")
    private Long expertApplicationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ExpertApplicationStatus status = ExpertApplicationStatus.NONE;

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

    public static ExpertApplication create(User user, String documentUrl) {
        ExpertApplication application = new ExpertApplication();
        application.user = user;
        application.applicationDocumentUrl = documentUrl;
        application.status = ExpertApplicationStatus.PENDING;
        application.appliedAt = LocalDateTime.now();
        return application;
    }

    /**
     * PENDING 상태 신청의 첨부 파일을 새 파일로 교체하고 신청 시각을 갱신한다.
     */
    public void replaceDocument(String documentUrl) {
        this.applicationDocumentUrl = documentUrl;
        this.appliedAt = LocalDateTime.now();
    }
}
