package com.nodatanolife.bifusion_be.job;

import com.nodatanolife.bifusion_be.domain.job.dto.response.JobResponse;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationConfig;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationParameters;
import com.nodatanolife.bifusion_be.domain.job.entity.Job;
import com.nodatanolife.bifusion_be.domain.job.entity.JobFailureReason;
import com.nodatanolife.bifusion_be.domain.job.entity.JobType;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.response.MlAugmentationResult;
import com.nodatanolife.bifusion_be.domain.job.repository.AugmentationConfigRepository;
import com.nodatanolife.bifusion_be.domain.job.repository.JobRepository;
import com.nodatanolife.bifusion_be.domain.job.service.JobCommandService;
import com.nodatanolife.bifusion_be.domain.job.service.JobPollingService;
import com.nodatanolife.bifusion_be.domain.job.service.JobQueryService;
import com.nodatanolife.bifusion_be.domain.job.service.MlConnectionState;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.support.IntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 증강 작업 완료·실패 알림, Job 응답의 ML 접수 시각·연결 불가 상태.
 */
class JobResultIntegrationTest extends IntegrationTestSupport {

    @Autowired
    JobRepository jobRepository;
    @Autowired
    AugmentationConfigRepository augmentationConfigRepository;
    @Autowired
    JobPollingService jobPollingService;
    @Autowired
    JobCommandService jobCommandService;
    @Autowired
    JobQueryService jobQueryService;
    @Autowired
    MlConnectionState mlConnectionState;

    @AfterEach
    void resetMlState() {
        mlConnectionState.markReachable();
    }

    @Test
    void 증강_작업이_성공하면_실행한_사용자에게_완료_알림이_간다() {
        User owner = user("owner@test.com", 0);
        Job job = startedJob(owner, JobType.AUGMENTATION);
        augmentationConfigRepository.save(AugmentationConfig.create(job, new AugmentationParameters(10, 0.3, 400, 50, 1L),
                List.of(1L), List.of(2L)));

        jobPollingService.applyAugmentationSuccess(job.getJobId(), new MlAugmentationResult("ds-1", 1, 1, 2, 2, null), null, null);

        assertThat(notifications(owner)).containsExactly(Map.of("type", "AUGMENTATION_SUCCESS", "target_type", "JOB",
                "target_id", job.getJobId()));
    }

    @Test
    void 증강_작업이_실패하면_사용자용_실패_문구와_함께_실패_알림이_간다() {
        User owner = user("owner@test.com", 0);
        Job job = startedJob(owner, JobType.AUGMENTATION);

        jobPollingService.fail(job.getJobId(), JobFailureReason.ML_UNREACHABLE, "traceback 원문");

        String content = jdbc.queryForObject("SELECT content FROM notification WHERE user_id = ? AND type = 'AUGMENTATION_FAILED'",
                String.class, owner.getUserId());
        assertThat(content).contains(JobFailureReason.ML_UNREACHABLE.getMessage()).doesNotContain("traceback");
    }

    @Test
    void Job_응답에_ML_접수_시각과_연결_불가_시각이_나오고_끝난_작업에는_연결_불가가_나오지_않는다() {
        User owner = user("owner@test.com", 0);
        Job job = startedJob(owner, JobType.AUGMENTATION);

        JobResponse connected = jobQueryService.getJob(owner.getUserId(), job.getJobId());
        assertThat(connected.startedAt()).isNotNull();
        assertThat(connected.mlUnreachableSince()).isNull();

        LocalDateTime since = LocalDateTime.now().minusMinutes(1);
        mlConnectionState.markUnreachable(since);
        assertThat(jobQueryService.getJob(owner.getUserId(), job.getJobId()).mlUnreachableSince()).isEqualTo(since);

        jobPollingService.fail(job.getJobId(), JobFailureReason.ML_UNREACHABLE, null);
        assertThat(jobQueryService.getJob(owner.getUserId(), job.getJobId()).mlUnreachableSince()).isNull();
    }

    private Job startedJob(User owner, JobType type) {
        Job job = Job.create(projectEntity(owner), owner, type, 1L, "idem-" + System.nanoTime());
        job.start("ml-" + System.nanoTime());
        return jobRepository.save(job);
    }

    private com.nodatanolife.bifusion_be.domain.project.entity.Project projectEntity(User owner) {
        return projectRepository.findById(project(owner)).orElseThrow();
    }

    private List<Map<String, Object>> notifications(User user) {
        return jdbc.queryForList("SELECT type, target_type, target_id FROM notification WHERE user_id = ?", user.getUserId());
    }
}
