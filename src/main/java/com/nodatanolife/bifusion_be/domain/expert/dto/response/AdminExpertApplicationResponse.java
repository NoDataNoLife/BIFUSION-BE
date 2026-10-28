package com.nodatanolife.bifusion_be.domain.expert.dto.response;

import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertApplication;
import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertApplicationStatus;
import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
import com.nodatanolife.bifusion_be.domain.user.entity.User;

import java.time.LocalDateTime;

/**
 * 관리자 심사 화면용 전문가 인증 신청.
 *
 * @param documentUrl 신청자가 올린 인증 서류 (presigned URL)
 */
public record AdminExpertApplicationResponse(
        Long applicationId,
        Long userId,
        String nickname,
        String email,
        ExpertApplicationStatus status,
        @PresignedUrl String documentUrl,
        LocalDateTime appliedAt,
        LocalDateTime reviewedAt,
        String rejectionReason
) {
    public static AdminExpertApplicationResponse from(ExpertApplication application) {
        User user = application.getUser();
        return new AdminExpertApplicationResponse(
                application.getExpertApplicationId(),
                user.getUserId(),
                user.getNickname(),
                user.getEmail(),
                application.getStatus(),
                application.getApplicationDocumentUrl(),
                application.getAppliedAt(),
                application.getReviewedAt(),
                application.getRejectionReason()
        );
    }
}
