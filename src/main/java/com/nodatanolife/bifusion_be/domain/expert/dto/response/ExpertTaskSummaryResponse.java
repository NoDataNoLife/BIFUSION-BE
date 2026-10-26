package com.nodatanolife.bifusion_be.domain.expert.dto.response;

import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertTask;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequest;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequestStatus;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;
import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
import com.nodatanolife.bifusion_be.domain.user.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @param targetTitle 검수 대상 이름. 증강 데이터는 프로젝트명, 레시피·데이터셋은 각 제목
 * @param targetImageUrl 검수 대상 대표 이미지. 증강 데이터는 ML 미리보기 이미지(AugmentationConfig.previewKey), 레시피는 썸네일, 데이터셋은 배너 (없으면 null)
 * @param projectName 증강 데이터 검수에만 채운다 (레시피·데이터셋은 null). targetTitle과 같은 값
 */
public record ExpertTaskSummaryResponse(
        String reviewCode,
        String status,
        InspectionRequestStatus reviewResult,
        RequesterResponse requester,
        InspectionTargetType targetType,
        String targetTitle,
        @PresignedUrl String targetImageUrl,
        String projectName,
        LocalDate requestedAt,
        LocalDateTime updatedAt
) {
    /**
     * @param targetTitle    대상을 찾지 못하면 null ("알 수 없는 대상"으로 표시)
     * @param targetImageUrl 대상이 없거나 이미지가 없으면 null
     */
    public static ExpertTaskSummaryResponse of(ExpertTask task, String targetTitle, String targetImageUrl) {
        InspectionRequest inspectionRequest = task.getInspectionRequest();
        InspectionTargetType targetType = inspectionRequest.getTargetType();
        boolean augmented = targetType == InspectionTargetType.AUGMENTED_DATA;
        String title = targetTitle != null ? targetTitle : (augmented ? "알 수 없는 프로젝트" : "알 수 없는 대상");
        return new ExpertTaskSummaryResponse(
                task.getReviewCode(),
                task.getStatus(),
                inspectionRequest.getStatus() == InspectionRequestStatus.PENDING ? null : inspectionRequest.getStatus(),
                RequesterResponse.from(inspectionRequest.getUser()),
                targetType,
                title,
                targetImageUrl,
                augmented ? title : null,
                inspectionRequest.getAppliedAt() != null ? inspectionRequest.getAppliedAt().toLocalDate() : null,
                task.getUpdatedAt()
        );
    }

    public record RequesterResponse(Long userId, String nickname, String email) {
        public static RequesterResponse from(User user) {
            if (user == null) {
                return new RequesterResponse(null, "알 수 없는 사용자", null);
            }
            return new RequesterResponse(user.getUserId(), user.getNickname(), user.getEmail());
        }
    }
}
