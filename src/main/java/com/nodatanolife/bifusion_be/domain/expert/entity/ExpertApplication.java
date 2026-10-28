package com.nodatanolife.bifusion_be.domain.expert.entity;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
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

    /**
     * 관리자가 PENDING 신청을 승인한다.
     */
    public void approve(Long adminId) {
        validatePending();
        this.status = ExpertApplicationStatus.APPROVED;
        this.reviewedBy = adminId;
        this.reviewedAt = LocalDateTime.now();
    }

    /**
     * 관리자가 PENDING 신청을 반려한다. 반려 사유는 알림으로 신청자에게 전달된다.
     */
    public void reject(Long adminId, String reason) {
        validatePending();
        this.status = ExpertApplicationStatus.REJECTED;
        this.rejectionReason = reason;
        this.reviewedBy = adminId;
        this.reviewedAt = LocalDateTime.now();
    }

    private void validatePending() {
        if (this.status != ExpertApplicationStatus.PENDING) {
            throw new BusinessException(ErrorCode.EXPERT_APPLICATION_NOT_PENDING);
        }
    }
}
