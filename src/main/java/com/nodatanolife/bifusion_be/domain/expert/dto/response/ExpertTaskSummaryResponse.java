package com.nodatanolife.bifusion_be.domain.expert.dto.response;

import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertTask;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequest;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequestStatus;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;
import com.nodatanolife.bifusion_be.domain.user.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @param targetTitle 검수 대상 이름. 증강 데이터는 프로젝트명, 레시피·데이터셋은 각 제목
 * @param projectName 증강 데이터 검수에만 채운다 (레시피·데이터셋은 null). targetTitle과 같은 값
 */
public record ExpertTaskSummaryResponse(
        String reviewCode,
        String status,
        InspectionRequestStatus reviewResult,
        RequesterResponse requester,
        InspectionTargetType targetType,
        String targetTitle,
        String projectName,
        LocalDate requestedAt,
        LocalDateTime updatedAt
) {
    /**
     * @param targetTitle 대상을 찾지 못하면 null ("알 수 없는 대상"으로 표시)
     */
    public static ExpertTaskSummaryResponse of(ExpertTask task, String targetTitle) {
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
