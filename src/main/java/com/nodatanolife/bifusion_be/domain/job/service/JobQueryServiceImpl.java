package com.nodatanolife.bifusion_be.domain.job.service;

import com.nodatanolife.bifusion_be.domain.job.dto.response.AugmentationJobResult;
import com.nodatanolife.bifusion_be.domain.job.dto.response.InferenceJobResult;
import com.nodatanolife.bifusion_be.domain.job.dto.response.JobResponse;
import com.nodatanolife.bifusion_be.domain.job.dto.response.JobResult;
import com.nodatanolife.bifusion_be.domain.job.dto.response.TrainingJobResult;
import com.nodatanolife.bifusion_be.domain.job.entity.Job;
import com.nodatanolife.bifusion_be.domain.job.entity.JobStatus;
import com.nodatanolife.bifusion_be.domain.job.repository.AugmentationConfigRepository;
import com.nodatanolife.bifusion_be.domain.job.repository.InferenceResultRepository;
import com.nodatanolife.bifusion_be.domain.job.repository.JobRepository;
import com.nodatanolife.bifusion_be.domain.job.repository.TrainedModelRepository;
import com.nodatanolife.bifusion_be.domain.job.repository.TrainingConfigRepository;
import com.nodatanolife.bifusion_be.domain.project.repository.ProjectMemberRepository;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobQueryServiceImpl implements JobQueryService {

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
