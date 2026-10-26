package com.nodatanolife.bifusion_be.job;

import com.nodatanolife.bifusion_be.domain.file.entity.File;
import com.nodatanolife.bifusion_be.domain.job.dto.request.JobCreateRequest;
import com.nodatanolife.bifusion_be.domain.job.dto.response.JobCostEstimateResponse;
import com.nodatanolife.bifusion_be.domain.job.entity.Job;
import com.nodatanolife.bifusion_be.domain.job.entity.JobFailureReason;
import com.nodatanolife.bifusion_be.domain.job.entity.JobType;
import com.nodatanolife.bifusion_be.domain.job.repository.JobRepository;
import com.nodatanolife.bifusion_be.domain.job.service.JobCommandService;
import com.nodatanolife.bifusion_be.domain.job.service.JobCostEstimateService;
import com.nodatanolife.bifusion_be.domain.job.service.JobLaunchPlan;
import com.nodatanolife.bifusion_be.domain.job.service.JobPollingService;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectRole;
import com.nodatanolife.bifusion_be.domain.user.entity.RewardHistory;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import com.nodatanolife.bifusion_be.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AI 작업 실행 비용 차감(생성·재시도), 실패·취소 환불, 예상 비용 조회.
 * 기본 설정 기준 비용: 증강 50, 학습 16, 추론 2 + 이미지 수.
 */
class JobPointIntegrationTest extends IntegrationTestSupport {

    @Autowired
    JobCommandService jobCommandService;
    @Autowired
    JobPollingService jobPollingService;
    @Autowired
    JobCostEstimateService jobCostEstimateService;
    @Autowired
    JobRepository jobRepository;

    // ---------- 차감 ----------

    @Test
    void 증강_작업을_만들면_기본_설정_비용_50앤트가_차감되고_작업에_기록된다() {
        User owner = user("owner@test.com", 100);
        Long projectId = project(owner);

        JobLaunchPlan plan = jobCommandService.createPending(owner.getUserId(), projectId, augmentation(owner, Map.of()), null);

        assertThat(pointsOf(owner)).isEqualTo(50);
        assertThat(rewardHistoryAmount(owner, RewardHistory.TYPE_JOB_RUN)).isEqualTo(-50);
        assertThat(jobRepository.findById(plan.jobId()).orElseThrow().getChargedPoints()).isEqualTo(50);
    }

    @Test
    void 설정값이_클수록_비용이_커진다() {
        User owner = user("owner@test.com", 1000);
        Long projectId = project(owner);

        // 20 + ceil(1000/20) + ceil(2*100*200/100) = 20 + 50 + 400
        jobCommandService.createPending(owner.getUserId(), projectId,
                augmentation(owner, Map.of("num_samples_per_class", 100, "finetune_steps", 1000, "steps", 200)), null);

        assertThat(pointsOf(owner)).isEqualTo(1000 - 470);
    }

    @Test
    void 앤트가_부족하면_작업을_만들지_않고_이미지도_임시_파일로_남는다() {
        User owner = user("owner@test.com", 49);
        Long projectId = project(owner);
        JobCreateRequest request = augmentation(owner, Map.of());

        assertThatThrownBy(() -> jobCommandService.createPending(owner.getUserId(), projectId, request, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_POINTS);

        assertThat(pointsOf(owner)).isEqualTo(49);
        assertThat(jobRepository.count()).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM file WHERE is_temp = false", Integer.class)).isZero();
    }

    @Test
    void 같은_멱등_키로_다시_요청하면_한_번만_차감된다() {
        User owner = user("owner@test.com", 100);
        Long projectId = project(owner);
        JobCreateRequest request = augmentation(owner, Map.of());

        JobLaunchPlan first = jobCommandService.createPending(owner.getUserId(), projectId, request, "idem-1");
        JobLaunchPlan second = jobCommandService.createPending(owner.getUserId(), projectId, request, "idem-1");

        assertThat(second.existing()).isTrue();
        assertThat(second.jobId()).isEqualTo(first.jobId());
        assertThat(pointsOf(owner)).isEqualTo(50);
        assertThat(rewardHistoryCount(owner, RewardHistory.TYPE_JOB_RUN)).isEqualTo(1);
    }

    // ---------- 환불 ----------

    @Test
    void 실행_요청_단계에서_실패하면_전액_환불된다() {
        User owner = user("owner@test.com", 100);
        JobLaunchPlan plan = jobCommandService.createPending(owner.getUserId(), project(owner), augmentation(owner, Map.of()), null);

        jobCommandService.markFailed(plan.jobId(), JobFailureReason.ML_UNAVAILABLE);

        assertThat(pointsOf(owner)).isEqualTo(100);
        assertThat(rewardHistoryAmount(owner, RewardHistory.TYPE_JOB_REFUND)).isEqualTo(50);
    }

    @Test
    void 입력_오류로_실패해도_전액_환불된다() {
        User owner = user("owner@test.com", 100);
        JobLaunchPlan plan = jobCommandService.createPending(owner.getUserId(), project(owner), augmentation(owner, Map.of()), null);
        jobCommandService.markStarted(plan.jobId(), "ml-1");

        jobPollingService.fail(plan.jobId(), JobFailureReason.NORMAL_IMAGE_INVALID, null);

        assertThat(pointsOf(owner)).isEqualTo(100);
    }

    @Test
    void 취소하면_전액_환불된다() {
        User owner = user("owner@test.com", 100);
        JobLaunchPlan plan = jobCommandService.createPending(owner.getUserId(), project(owner), augmentation(owner, Map.of()), null);
        jobCommandService.markStarted(plan.jobId(), "ml-1");

        jobCommandService.applyCancel(plan.jobId());
        // 이미 취소된 작업에 다시 반영해도 두 번 환불하지 않는다
        jobCommandService.applyCancel(plan.jobId());

        assertThat(pointsOf(owner)).isEqualTo(100);
        assertThat(rewardHistoryCount(owner, RewardHistory.TYPE_JOB_REFUND)).isEqualTo(1);
    }

    @Test
    void 끝난_작업에_실패를_다시_반영해도_두_번_환불하지_않는다() {
        User owner = user("owner@test.com", 100);
        JobLaunchPlan plan = jobCommandService.createPending(owner.getUserId(), project(owner), augmentation(owner, Map.of()), null);
        jobCommandService.markStarted(plan.jobId(), "ml-1");

        jobPollingService.fail(plan.jobId(), JobFailureReason.UNKNOWN, null);
        jobPollingService.fail(plan.jobId(), JobFailureReason.UNKNOWN, null);

        assertThat(pointsOf(owner)).isEqualTo(100);
        assertThat(rewardHistoryCount(owner, RewardHistory.TYPE_JOB_REFUND)).isEqualTo(1);
    }

    @Test
    void 차감_도입_전에_만든_작업은_실패해도_환불하지_않는다() {
        User owner = user("owner@test.com", 0);
        Job legacy = Job.create(projectRepository.findById(project(owner)).orElseThrow(), owner, JobType.AUGMENTATION, 1L, null);
        legacy.start("ml-legacy");
        jobRepository.save(legacy);

        jobPollingService.fail(legacy.getJobId(), JobFailureReason.UNKNOWN, null);

        assertThat(pointsOf(owner)).isZero();
        assertThat(rewardHistoryCount(owner, RewardHistory.TYPE_JOB_REFUND)).isZero();
    }

    // ---------- 재시도 ----------

    @Test
    void 재시도하면_재시도한_사람에게서_같은_금액이_다시_차감된다() {
        User owner = user("owner@test.com", 100);
        User member = user("member@test.com", 100);
        Long projectId = project(owner);
        addMember(projectId, member, ProjectRole.MEMBER);
        JobLaunchPlan plan = jobCommandService.createPending(owner.getUserId(), projectId, augmentation(owner, Map.of()), null);
        jobCommandService.markFailed(plan.jobId(), JobFailureReason.ML_UNAVAILABLE);

        JobLaunchPlan retry = jobCommandService.createRetry(member.getUserId(), plan.jobId(), null);

        assertThat(pointsOf(owner)).isEqualTo(100);
        assertThat(pointsOf(member)).isEqualTo(50);
        assertThat(jobRepository.findById(retry.jobId()).orElseThrow().getChargedPoints()).isEqualTo(50);
    }

    @Test
    void 앤트가_부족하면_재시도가_막히고_원본은_다시_시도할_수_있는_상태로_남는다() {
        User owner = user("owner@test.com", 50);
        JobLaunchPlan plan = jobCommandService.createPending(owner.getUserId(), project(owner), augmentation(owner, Map.of()), null);
        jobCommandService.markFailed(plan.jobId(), JobFailureReason.ML_UNAVAILABLE);
        jdbc.update("UPDATE users SET reward_balance = 10 WHERE user_id = ?", owner.getUserId());

        assertThatThrownBy(() -> jobCommandService.createRetry(owner.getUserId(), plan.jobId(), null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_POINTS);

        assertThat(jobRepository.findById(plan.jobId()).orElseThrow().isRetryable()).isTrue();
        assertThat(jobRepository.count()).isEqualTo(1);
    }

    // ---------- 예상 비용 ----------

    @Test
    void 예상_비용은_생략한_설정값을_기본값으로_채워_계산하고_이미지가_없어도_된다() {
        User owner = user("owner@test.com", 30);

        JobCostEstimateResponse augmentation = jobCostEstimateService.estimate(owner.getUserId(),
                request(JobCreateRequest.class, Map.of("type", "AUGMENTATION")));
        JobCostEstimateResponse training = jobCostEstimateService.estimate(owner.getUserId(),
                request(JobCreateRequest.class, Map.of("type", "TRAINING")));

        assertThat(augmentation.cost()).isEqualTo(50);
        assertThat(augmentation.currentPoints()).isEqualTo(30);
        assertThat(augmentation.affordable()).isFalse();
        // 10 + ceil(30 * 10 / 50)
        assertThat(training.cost()).isEqualTo(16);
        assertThat(training.affordable()).isTrue();
    }

    @Test
    void 추론_예상_비용은_이미지_수에_비례한다() {
        User owner = user("owner@test.com", 0);

        JobCostEstimateResponse response = jobCostEstimateService.estimate(owner.getUserId(),
                request(JobCreateRequest.class, Map.of("type", "INFERENCE", "model_id", 1, "fileIds", List.of(1, 2, 3))));

        assertThat(response.cost()).isEqualTo(5);
    }

    @Test
    void 예상_비용도_실행과_같은_범위로_검증한다() {
        User owner = user("owner@test.com", 0);

        assertThatThrownBy(() -> jobCostEstimateService.estimate(owner.getUserId(),
                request(JobCreateRequest.class, Map.of("type", "AUGMENTATION", "steps", 201)))).isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_JOB_PARAMETER);
        assertThatThrownBy(() -> jobCostEstimateService.estimate(owner.getUserId(),
                request(JobCreateRequest.class, Map.of("type", "INFERENCE", "model_id", 1)))).isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_IMAGE_COUNT);
    }

    @Test
    void 예상_비용과_실제_차감액이_같다() {
        User owner = user("owner@test.com", 1000);
        Map<String, Object> settings = Map.of("num_samples_per_class", 37, "finetune_steps", 333, "steps", 77);
        Map<String, Object> estimateBody = new HashMap<>(settings);
        estimateBody.put("type", "AUGMENTATION");

        int estimated = jobCostEstimateService.estimate(owner.getUserId(), request(JobCreateRequest.class, estimateBody)).cost();
        jobCommandService.createPending(owner.getUserId(), project(owner), augmentation(owner, settings), null);

        assertThat(pointsOf(owner)).isEqualTo(1000 - estimated);
    }

    // ---------- fixtures ----------

    /**
     * 정상·비정상 이미지 5장씩을 올린 증강 요청. settings로 설정값을 덮어쓴다.
     */
    private JobCreateRequest augmentation(User owner, Map<String, Object> settings) {
        Map<String, Object> body = new HashMap<>(settings);
        body.put("type", "AUGMENTATION");
        body.put("normalFileIds", images(owner));
        body.put("anomalyFileIds", images(owner));
        return request(JobCreateRequest.class, body);
    }

    private List<Long> images(User owner) {
        return IntStream.range(0, 5)
                .mapToObj(i -> tempFile(owner, "image/png", 1024))
                .map(File::getFileId)
                .toList();
    }
}
