package com.nodatanolife.bifusion_be.domain.job.entity;

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
        uniqueConstraints = @UniqueConstraint(
                name = "uk_job_user_idempotency_key",
                columnNames = {"user_id", "idempotency_key"}
        )
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

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private JobType type;

    @Enumerated(EnumType.STRING)
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

    @Version
    @Column(name = "version")
    private Long version;

    @Builder
    private Job(Project project, User user, JobType type, String seedNumber, String idempotencyKey) {
        this.project = project;
        this.user = user;
        this.type = type;
        this.seedNumber = seedNumber;
        this.idempotencyKey = idempotencyKey;
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
