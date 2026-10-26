package com.nodatanolife.bifusion_be.domain.job.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.nodatanolife.bifusion_be.domain.job.entity.Job;
import com.nodatanolife.bifusion_be.domain.job.entity.JobStatus;
import com.nodatanolife.bifusion_be.domain.job.entity.JobType;

import java.time.LocalDateTime;

/**
 * AI Job 생성·조회 응답. FE parseJob 형식을 따른다.
 * error는 사용자용 문구(errorMessage)만 담고, ML 원문(mlErrorDetail)은 절대 포함하지 않는다.
 * retryable이 true일 때만 FE가 "다시 시도"를 보여준다. retryOfJobId는 이 Job이 재시도한 원본 jobId(없으면 null).
 */
@Schema(description = "AI 작업 상태·결과 (조회·실행·재시도·취소 응답 공통)")
public record JobResponse(
        @Schema(description = "작업 번호", example = "15")
        Long jobId,
        @Schema(description = "작업 종류", example = "AUGMENTATION")
        JobType type,
        @Schema(description = "프로젝트 번호", example = "1")
        Long projectId,
        @Schema(description = "작업 상태", example = "RUNNING")
        JobStatus status,
        @Schema(description = "진행률. 현재 사용하지 않아 항상 null (진행 표시는 currentStep 사용)")
        Integer progress,
        @Schema(description = "진행 중 단계 설명 (ML이 보내는 경우만, 없으면 null)", example = "Fine-tuning normal 40%")
        String currentStep,
        @Schema(description = "실제 사용한 시드", example = "42")
        String seedNumber,
        @Schema(description = "작업 결과. SUCCESS일 때만 채워지고, 종류별 형식이 다름 (AugmentationJobResult / TrainingJobResult / InferenceJobResult)")
        JobResult result,
        @Schema(description = "실패 시 사용자에게 보여줄 문구 (FAILED일 때만)", example = "AI 서버에 연결할 수 없어 작업을 시작하지 못했습니다. 잠시 후 다시 시도해 주세요.")
        String error,
        @Schema(description = "다시 시도 가능 여부. true일 때만 \"다시 시도\" 버튼 표시", example = "false")
        boolean retryable,
        @Schema(description = "이 작업이 다시 시도한 원본 작업 번호 (없으면 null)")
        Long retryOfJobId,
        @Schema(description = "실행 시 차감한 앤트. 실패·취소되면 전액 돌려받습니다. 차감 도입 전 작업은 null", example = "50")
        Integer chargedPoints,
        @Schema(description = "생성 시각")
        LocalDateTime createdAt,
        @Schema(description = "AI 서버 접수 시각. 대기(PENDING) 시간은 이 값부터 잰다. 접수 전이면 null")
        LocalDateTime startedAt,
        @Schema(description = "AI 서버 연결이 끊긴 시각. 진행 중(PENDING·RUNNING)인 작업이고 지금 연결이 끊겨 있을 때만 값이 있다. "
                + "연결이 1분(ml.polling.unreachable-timeout) 넘게 끊기면 작업은 실패(FAILED, ML_UNREACHABLE) 처리된다")
        LocalDateTime mlUnreachableSince,
        @Schema(description = "마지막 변경 시각")
        LocalDateTime updatedAt
) {

    /**
     * @param mlUnreachableSince 현재 ML 연결 불가 시작 시각 (MlConnectionState). 연결돼 있으면 null
     */
    public static JobResponse from(Job job, JobResult result, LocalDateTime mlUnreachableSince) {
        return new JobResponse(
                job.getJobId(),
                job.getType(),
                job.getProject().getProjectId(),
                job.getStatus(),
                job.getProgress(),
                job.getCurrentStep(),
                job.getSeedNumber(),
                job.getStatus() == JobStatus.SUCCESS ? result : null,
                job.getErrorMessage(),
                job.isRetryable(),
                job.getRetryOfJobId(),
                job.getChargedPoints(),
                job.getCreatedAt(),
                job.getStartedAt(),
                job.getStatus().isTerminal() ? null : mlUnreachableSince,
                job.getUpdatedAt()
        );
    }
}
