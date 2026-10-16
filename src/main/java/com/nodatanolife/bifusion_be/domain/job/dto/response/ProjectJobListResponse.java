package com.nodatanolife.bifusion_be.domain.job.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
import com.nodatanolife.bifusion_be.domain.job.entity.Job;
import com.nodatanolife.bifusion_be.domain.job.entity.JobStatus;
import com.nodatanolife.bifusion_be.domain.job.entity.JobType;
import com.nodatanolife.bifusion_be.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 프로젝트 AI Job 목록 (커서 기반, 최신순).
 *
 * @param statusCounts 상태별 개수. 첫 페이지에서만 담기며(다음 페이지는 null), type·keyword 필터는 반영하고 status 필터는 반영하지 않는다.
 */
@Schema(description = "프로젝트 AI 작업 목록 (최신순, 커서 기반)")
public record ProjectJobListResponse(
        @Schema(description = "작업 목록")
        List<Item> items,
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext,
        @Schema(description = "다음 페이지 요청 시 cursor로 그대로 전달 (마지막 페이지면 null)")
        String nextCursor,
        @Schema(description = "상태별 개수. 첫 페이지에서만 내려가며(다음 페이지는 null), type·keyword 필터는 반영하고 status 필터는 반영하지 않음")
        Map<JobStatus, Long> statusCounts
) {

    public static ProjectJobListResponse of(List<Job> jobs, boolean hasNext, String nextCursor,
                                            Map<JobStatus, Long> statusCounts) {
        return new ProjectJobListResponse(jobs.stream().map(Item::from).toList(), hasNext, nextCursor, statusCounts);
    }

    /**
     * @param error 사용자용 실패 문구만 담는다. ML 원문(mlErrorDetail)은 포함하지 않는다.
     */
    @Schema(name = "ProjectJobItem", description = "목록의 작업 1건")
    public record Item(
            @Schema(description = "작업 번호", example = "15")
            Long jobId,
            @Schema(description = "화면 표시용 코드", example = "JOB-015")
            String jobCode,
            @Schema(description = "작업 종류", example = "AUGMENTATION")
            JobType type,
            @Schema(description = "작업 상태 (조회 시점 값)", example = "RUNNING")
            JobStatus status,
            @Schema(description = "시드", example = "42")
            String seedNumber,
            @Schema(description = "실행자")
            CreatedBy createdBy,
            @Schema(description = "생성 시각")
            LocalDateTime createdAt,
            @Schema(description = "종료 시각 (진행 중이면 null)")
            LocalDateTime finishedAt,
            @Schema(description = "진행 중 단계 설명 (없으면 null)", example = "Fine-tuning normal 40%")
            String currentStep,
            @Schema(description = "실패 시 사용자에게 보여줄 문구")
            String error,
            @Schema(description = "다시 시도 가능 여부", example = "false")
            boolean retryable,
            @Schema(description = "다시 시도한 원본 작업 번호 (없으면 null)")
            Long retryOfJobId
    ) {

        public static Item from(Job job) {
            return new Item(
                    job.getJobId(),
                    job.getDisplayCode(),
                    job.getType(),
                    job.getStatus(),
                    job.getSeedNumber(),
                    CreatedBy.from(job.getUser()),
                    job.getCreatedAt(),
                    job.getFinishedAt(),
                    job.getCurrentStep(),
                    job.getErrorMessage(),
                    job.isRetryable(),
                    job.getRetryOfJobId()
            );
        }
    }

    /**
     * 실행자. 탈퇴한 사용자의 Job은 공용 탈퇴 계정(User.createDeletedUser, role=DELETED)으로 바뀌어 있으며,
     * 이 경우 프로필 링크가 걸리지 않도록 userId는 null로 내린다 (닉네임은 "탈퇴한 사용자").
     */
    @Schema(name = "JobCreatedBy", description = "작업 실행자")
    public record CreatedBy(
            @Schema(description = "실행자 번호. 탈퇴한 사용자면 null", example = "11")
            Long userId,
            @Schema(description = "실행자 닉네임. 탈퇴한 사용자면 \"탈퇴한 사용자\"", example = "홍길동")
            String nickname,
            @Schema(description = "프로필 이미지 주소 (presigned, 없으면 null)")
            @PresignedUrl String profileImageUrl
    ) {

        private static final String DELETED_USER_ROLE = "DELETED";

        public static CreatedBy from(User user) {
            boolean deleted = DELETED_USER_ROLE.equals(user.getRole());
            return new CreatedBy(deleted ? null : user.getUserId(), user.getNickname(), user.getProfileImageUrl());
        }
    }
}
