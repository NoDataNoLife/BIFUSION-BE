package com.nodatanolife.bifusion_be.domain.job.service;

import com.nodatanolife.bifusion_be.domain.job.dto.response.AugmentationJobResult;
import com.nodatanolife.bifusion_be.domain.job.dto.response.InferenceJobResult;
import com.nodatanolife.bifusion_be.domain.job.dto.response.JobResponse;
import com.nodatanolife.bifusion_be.domain.job.dto.response.JobResult;
import com.nodatanolife.bifusion_be.domain.job.dto.response.ProjectJobListResponse;
import com.nodatanolife.bifusion_be.domain.job.dto.response.TrainingJobResult;
import com.nodatanolife.bifusion_be.domain.job.entity.Job;
import com.nodatanolife.bifusion_be.domain.job.entity.JobStatus;
import com.nodatanolife.bifusion_be.domain.job.entity.JobType;
import com.nodatanolife.bifusion_be.domain.job.repository.AugmentationConfigRepository;
import com.nodatanolife.bifusion_be.domain.job.repository.InferenceResultRepository;
import com.nodatanolife.bifusion_be.domain.job.repository.JobRepository;
import com.nodatanolife.bifusion_be.domain.job.repository.TrainedModelRepository;
import com.nodatanolife.bifusion_be.domain.job.repository.TrainingConfigRepository;
import com.nodatanolife.bifusion_be.domain.project.repository.ProjectMemberRepository;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobQueryServiceImpl implements JobQueryService {

    private static final int MAX_PAGE_SIZE = 50;

    private final JobRepository jobRepository;
    private final AugmentationConfigRepository augmentationConfigRepository;
    private final TrainingConfigRepository trainingConfigRepository;
    private final TrainedModelRepository trainedModelRepository;
    private final InferenceResultRepository inferenceResultRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Override
    public JobResponse getJob(Long userId, Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_NOT_FOUND));
        return toResponse(userId, job);
    }

    @Override
    public JobResponse getJobByIdempotencyKey(Long userId, String idempotencyKey) {
        Job job = jobRepository.findByUserUserIdAndIdempotencyKey(userId, idempotencyKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_NOT_FOUND));
        return toResponse(userId, job);
    }

    /**
     * size+1개를 조회해 다음 페이지 여부를 판단한다 (count 쿼리 없음).
     * 상태별 개수는 status 필터를 빼고 같은 검색 조건으로 group by 한 번 조회한다.
     */
    @Override
    public ProjectJobListResponse getProjectJobs(Long userId, Long projectId, JobType type, JobStatus status,
                                                 String keyword, String cursor, int size) {
        validateProjectMember(projectId, userId);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        JobSearchKeyword search = JobSearchKeyword.parse(keyword);
        JobListCursor after = JobListCursor.decode(cursor);

        List<Job> fetched = jobRepository.findProjectJobs(projectId,
                type != null, type,
                status != null, status,
                search.hasJobId(), search.jobId(),
                search.hasNickname(), search.nicknamePattern(),
                after != null, after != null ? after.createdAt() : null, after != null ? after.jobId() : null,
                PageRequest.of(0, pageSize + 1));

        boolean hasNext = fetched.size() > pageSize;
        List<Job> page = hasNext ? fetched.subList(0, pageSize) : fetched;
        String nextCursor = hasNext
                ? new JobListCursor(page.get(page.size() - 1).getCreatedAt(), page.get(page.size() - 1).getJobId()).encode()
                : null;
        Map<JobStatus, Long> statusCounts = after == null ? countByStatus(projectId, type, search) : null;
        return ProjectJobListResponse.of(page, hasNext, nextCursor, statusCounts);
    }

    /**
     * 5개 상태 모두 키를 담고, 해당 상태 Job이 없으면 0으로 채운다.
     */
    private Map<JobStatus, Long> countByStatus(Long projectId, JobType type, JobSearchKeyword search) {
        Map<JobStatus, Long> counts = new EnumMap<>(JobStatus.class);
        for (JobStatus s : JobStatus.values()) {
            counts.put(s, 0L);
        }
        jobRepository.countProjectJobsByStatus(projectId,
                        type != null, type,
                        search.hasJobId(), search.jobId(),
                        search.hasNickname(), search.nicknamePattern())
                .forEach(c -> counts.put(c.status(), c.count()));
        return counts;
    }

    private void validateProjectMember(Long projectId, Long userId) {
        if (!projectMemberRepository.existsByProjectProjectIdAndUserUserId(projectId, userId)) {
            throw new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED);
        }
    }

    private JobResponse toResponse(Long userId, Job job) {
        if (!projectMemberRepository.existsByProjectProjectIdAndUserUserId(job.getProject().getProjectId(), userId)) {
            throw new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED);
        }
        return JobResponse.from(job, job.getStatus() == JobStatus.SUCCESS ? findResult(job) : null);
    }

    private JobResult findResult(Job job) {
        Long jobId = job.getJobId();
        return switch (job.getType()) {
            case AUGMENTATION -> augmentationConfigRepository.findByJobJobId(jobId)
                    .map(AugmentationJobResult::from)
                    .orElse(null);
            case TRAINING -> trainingConfigRepository.findByJobJobId(jobId)
                    .map(config -> TrainingJobResult.from(config,
                            trainedModelRepository.findByTrainingJobJobId(jobId).orElse(null)))
                    .orElse(null);
            case INFERENCE -> InferenceJobResult.from(
                    inferenceResultRepository.findAllByInferenceConfigJobJobIdOrderBySampleIndexAsc(jobId));
        };
    }
}
