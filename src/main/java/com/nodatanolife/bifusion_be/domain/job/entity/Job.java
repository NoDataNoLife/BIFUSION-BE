package com.nodatanolife.bifusion_be.domain.job.entity;

import com.nodatanolife.bifusion_be.domain.job.entity.converter.JobFailureReasonConverter;
import com.nodatanolife.bifusion_be.domain.job.entity.converter.JobStatusConverter;
import com.nodatanolife.bifusion_be.domain.job.entity.converter.JobTypeConverter;
import com.nodatanolife.bifusion_be.domain.project.entity.Project;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * AI 작업(증강/학습/추론) 공통 라이프사이클.
 * 타입별 파라미터·결과는 AugmentationConfig / TrainingConfig / InferenceConfig에 둔다.
 *
 * 상태 전이: PENDING → (start) → PENDING → RUNNING → SUCCESS / FAILED / CANCELLED
 * 종료 상태에서는 어떤 전이도 허용하지 않으며, 완료·취소 동시 처리는 @Version 낙관적 락으로 한쪽만 반영된다.
 */
@Entity
@Table(
        name = "job",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_job_user_idempotency_key", columnNames = {"user_id", "idempotency_key"}),
                // 한 Job에서 재시도가 두 갈래로 생기지 않도록 (A → B → C 한 줄로만)
                @UniqueConstraint(name = "uk_job_retry_of_job_id", columnNames = {"retry_of_job_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Job extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "job_code", length = 20)
    private String jobCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 컨버터로 매핑: @Enumerated는 새 컬럼에 enum 값 목록 CHECK 제약을 만들어 값 추가 시 prod 저장이 막힌다.
    @Convert(converter = JobTypeConverter.class)
    @Column(name = "type", length = 20, nullable = false)
    private JobType type;

    @Convert(converter = JobStatusConverter.class)
    @Column(name = "status", length = 20, nullable = false)
    private JobStatus status;

    @Column(name = "progress")
    private Integer progress;

    @Column(name = "seed_number", length = 20)
    private String seedNumber;

    @Column(name = "ml_job_id", length = 100)
    private String mlJobId;

    // ML이 보내는 진행 문구. FE가 상태 설명으로 그대로 표시한다.
    @Column(name = "current_step", length = 500)
    private String currentStep;

    // 사용자에게 노출하는 실패 문구 (JobFailureReason)
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    // 실패 사유 코드. 재시도 가능 여부 판정에 쓴다. 이 컬럼 도입 전에 실패한 Job은 null(재시도 불가).
    @Convert(converter = JobFailureReasonConverter.class)
    @Column(name = "failure_reason", length = 50)
    private JobFailureReason failureReason;

    // ML error 원문. 내부 경로·traceback이 포함될 수 있어 어떤 응답 DTO에도 포함하지 않는다.
    @Column(name = "ml_error_detail", columnDefinition = "TEXT")
    private String mlErrorDetail;

    // 중복 실행 방지 키. (user_id, idempotency_key) unique
    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    // ML에 작업을 접수한 시각. PENDING 타임아웃 판정 기준
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    // 이 Job이 어떤 Job의 재시도인지 (원본). 재시도가 아니면 null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retry_of_job_id")
    private Job retryOfJob;

    // 이 Job을 원본으로 만든 재시도 Job의 id. 있으면 이 Job은 더 이상 재시도할 수 없다.
    // 조회 응답의 retryable을 추가 쿼리 없이 판정하기 위해 원본 쪽에도 기록한다.
    @Column(name = "retried_by_job_id")
    private Long retriedByJobId;

    @Version
    @Column(name = "version")
    private Long version;

    @Builder
    private Job(Project project, User user, JobType type, String seedNumber, String idempotencyKey, Job retryOfJob) {
        this.project = project;
        this.user = user;
        this.type = type;
        this.seedNumber = seedNumber;
        this.idempotencyKey = idempotencyKey;
        this.retryOfJob = retryOfJob;
        this.status = JobStatus.PENDING;
    }

    public static Job create(Project project, User user, JobType type, Long seed, String idempotencyKey) {
        return Job.builder()
                .project(project)
                .user(user)
                .type(type)
                .seedNumber(seed != null ? String.valueOf(seed) : null)
                .idempotencyKey(idempotencyKey)
                .build();
    }

    /**
     * 원본 Job과 같은 프로젝트·종류·시드로 재시도 Job을 만든다. 소유자는 재시도한 사용자다.
     * 파라미터·입력 파일은 호출 측에서 원본 Config를 그대로 복사한다.
     */
    public static Job createRetry(Job source, User user, String idempotencyKey) {
        return Job.builder()
                .project(source.getProject())
                .user(user)
                .type(source.getType())
                .seedNumber(source.getSeedNumber())
                .idempotencyKey(idempotencyKey)
                .retryOfJob(source)
                .build();
    }

    /**
     * ML에 작업 접수가 완료되어 ML job_id를 받은 시점에 호출한다. 상태는 ML 큐 대기(PENDING)로 유지된다.
     */
    public void start(String mlJobId) {
        validateNotTerminal();
        if (this.mlJobId != null) {
            throw new BusinessException(ErrorCode.JOB_INVALID_STATE);
        }
        this.mlJobId = mlJobId;
        this.startedAt = LocalDateTime.now();
    }

    public void markRunning() {
        validateStarted();
        this.status = JobStatus.RUNNING;
    }

    public void updateCurrentStep(String currentStep) {
        validateNotTerminal();
        this.currentStep = currentStep;
    }

    public void succeed() {
        validateStarted();
        this.status = JobStatus.SUCCESS;
        this.finishedAt = LocalDateTime.now();
    }

    /**
     * ML 접수 전(요청 형식 오류 등)에도 실패 처리할 수 있도록 mlJobId 유무는 검사하지 않는다.
     */
    public void fail(JobFailureReason reason, String mlErrorDetail) {
        validateNotTerminal();
        this.status = JobStatus.FAILED;
        this.failureReason = reason;
        this.errorMessage = reason.getMessage();
        this.mlErrorDetail = mlErrorDetail;
        this.finishedAt = LocalDateTime.now();
    }

    public void cancel() {
        validateNotTerminal();
        this.status = JobStatus.CANCELLED;
        this.finishedAt = LocalDateTime.now();
    }

    /**
     * ML 접수 후 PENDING 상태로 threshold 이상 머물렀는지 판정한다.
     * ML은 모르는 job_id에도 PENDING을 반환하므로(워커 미기동·Redis 초기화) 스케줄러가 이 값으로 실패 처리한다.
     */
    public boolean isPendingTimedOut(Duration threshold, LocalDateTime now) {
        return status == JobStatus.PENDING
                && startedAt != null
                && startedAt.plus(threshold).isBefore(now);
    }

    /**
     * 재시도할 수 있는지. 조회 응답의 retryable에 쓰며, 엔티티 필드만으로 판정한다(추가 쿼리 없음).
     * (FAILED이면서 재시도 가능 사유) 또는 CANCELLED이고, 아직 이 Job을 원본으로 한 재시도가 없어야 한다.
     */
    public boolean isRetryable() {
        if (retriedByJobId != null) {
            return false;
        }
        return status == JobStatus.CANCELLED
                || (status == JobStatus.FAILED && failureReason != null && failureReason.isRetryable());
    }

    /**
     * 재시도할 수 없으면 이유에 맞는 예외를 던진다. 판정 기준은 isRetryable()과 같다.
     */
    public void validateRetryable() {
        if (status != JobStatus.FAILED && status != JobStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.JOB_NOT_RETRYABLE);
        }
        if (retriedByJobId != null) {
            throw new BusinessException(ErrorCode.JOB_ALREADY_RETRIED);
        }
        if (status == JobStatus.FAILED && (failureReason == null || !failureReason.isRetryable())) {
            throw new BusinessException(ErrorCode.JOB_RETRY_NEEDS_NEW_INPUT);
        }
    }

    /**
     * 이 Job을 원본으로 재시도 Job이 만들어졌음을 기록한다. 재시도 Job 생성과 같은 트랜잭션에서 호출한다.
     */
    public void markRetriedBy(Job retryJob) {
        validateRetryable();
        if (retryJob.getRetryOfJob() != this) {
            throw new BusinessException(ErrorCode.JOB_INVALID_STATE);
        }
        this.retriedByJobId = retryJob.getJobId();
    }

    /**
     * 취소 요청을 받을 수 있는 상태인지 확인한다. CANCELLED는 같은 요청 반복을 허용하므로 통과한다.
     * mlJobId 없는 PENDING은 실행 요청이 진행 중이라, BE만 취소하면 접수된 ML 작업을 추적할 수 없어 막는다.
     */
    public void validateCancellable() {
        if (status == JobStatus.SUCCESS || status == JobStatus.FAILED) {
            throw new BusinessException(ErrorCode.JOB_ALREADY_FINISHED);
        }
        if (status == JobStatus.PENDING && mlJobId == null) {
            throw new BusinessException(ErrorCode.JOB_STARTING);
        }
    }

    public boolean isOwnedBy(Long userId) {
        return user.getUserId().equals(userId);
    }

    public Long getRetryOfJobId() {
        return retryOfJob != null ? retryOfJob.getJobId() : null;
    }

    /**
     * 화면 표시용 Job 코드 (예: JOB-015). Job 코드가 필요한 모든 응답은 이 메서드를 써서 형식을 맞춘다.
     * TODO: 프로젝트별 순번이 필요하면 이 메서드만 변경 (job_code 컬럼은 현재 사용하지 않음)
     */
    public String getDisplayCode() {
        return jobId != null ? "JOB-%03d".formatted(jobId) : null;
    }

    /**
     * ML 실행 요청 전에 서버가 죽어 mlJobId 없이 남은 PENDING Job인지 판정한다 (생성 시각 기준).
     */
    public boolean isLaunchTimedOut(Duration threshold, LocalDateTime now) {
        return status == JobStatus.PENDING
                && mlJobId == null
                && getCreatedAt() != null
                && getCreatedAt().plus(threshold).isBefore(now);
    }

    /**
     * RUNNING이던 Job을 ML이 잃어버렸는지 판정한다. ML이 PENDING을 돌려줄 때만 쓴다.
     * Celery는 모르는 job_id에도 PENDING을 반환하므로, RUNNING 이후 PENDING이 threshold 넘게 이어지면 유실로 본다.
     * TODO: ML이 모르는 job_id에 404를 주도록 바뀌면 제거 (404 → ML_JOB_NOT_FOUND로 처리됨)
     */
    public boolean isStaleSince(Duration threshold, LocalDateTime now) {
        return status == JobStatus.RUNNING
                && startedAt != null
                && startedAt.plus(threshold).isBefore(now);
    }

    /**
     * ML 응답과 관계없이 RUNNING 상태가 상한을 넘었는지 판정한다 (ML 접수 시각 기준, 워커 멈춤 대비).
     */
    public boolean isRunningTimedOut(Duration threshold, LocalDateTime now) {
        return status == JobStatus.RUNNING
                && startedAt != null
                && startedAt.plus(threshold).isBefore(now);
    }

    public void updateUser(User newUser) {
        this.user = newUser;
    }

    public void anonymize(User deletedUser) {
        this.user = deletedUser;
    }

    private void validateStarted() {
        validateNotTerminal();
        if (this.mlJobId == null) {
            throw new BusinessException(ErrorCode.JOB_INVALID_STATE);
        }
    }

    private void validateNotTerminal() {
        if (this.status.isTerminal()) {
            throw new BusinessException(ErrorCode.JOB_ALREADY_FINISHED);
        }
    }
}
