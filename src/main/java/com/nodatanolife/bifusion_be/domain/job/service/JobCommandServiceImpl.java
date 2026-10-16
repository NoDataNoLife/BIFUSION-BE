package com.nodatanolife.bifusion_be.domain.job.service;

import com.nodatanolife.bifusion_be.domain.file.entity.File;
import com.nodatanolife.bifusion_be.domain.file.repository.FileRepository;
import com.nodatanolife.bifusion_be.domain.job.dto.request.AugmentationJobCreateRequest;
import com.nodatanolife.bifusion_be.domain.job.dto.request.InferenceJobCreateRequest;
import com.nodatanolife.bifusion_be.domain.job.dto.request.JobCreateRequest;
import com.nodatanolife.bifusion_be.domain.job.dto.request.TrainingJobCreateRequest;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationConfig;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationParameters;
import com.nodatanolife.bifusion_be.domain.job.entity.InferenceConfig;
import com.nodatanolife.bifusion_be.domain.job.entity.Job;
import com.nodatanolife.bifusion_be.domain.job.entity.JobFailureReason;
import com.nodatanolife.bifusion_be.domain.job.entity.JobStatus;
import com.nodatanolife.bifusion_be.domain.job.entity.JobType;
import com.nodatanolife.bifusion_be.domain.job.entity.TrainedModel;
import com.nodatanolife.bifusion_be.domain.job.entity.TrainingConfig;
import com.nodatanolife.bifusion_be.domain.job.entity.TrainingParameters;
import com.nodatanolife.bifusion_be.domain.job.ml.MlProperties;
import com.nodatanolife.bifusion_be.domain.job.repository.AugmentationConfigRepository;
import com.nodatanolife.bifusion_be.domain.job.repository.InferenceConfigRepository;
import com.nodatanolife.bifusion_be.domain.job.repository.JobRepository;
import com.nodatanolife.bifusion_be.domain.job.repository.TrainedModelRepository;
import com.nodatanolife.bifusion_be.domain.job.repository.TrainingConfigRepository;
import com.nodatanolife.bifusion_be.domain.project.entity.Project;
import com.nodatanolife.bifusion_be.domain.project.repository.ProjectMemberRepository;
import com.nodatanolife.bifusion_be.domain.project.repository.ProjectRepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class JobCommandServiceImpl implements JobCommandService {

    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 100;

    private final JobRepository jobRepository;
    private final AugmentationConfigRepository augmentationConfigRepository;
    private final TrainingConfigRepository trainingConfigRepository;
    private final InferenceConfigRepository inferenceConfigRepository;
    private final TrainedModelRepository trainedModelRepository;
    private final FileRepository fileRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final JobRequestValidator jobRequestValidator;
    private final MlProperties mlProperties;

    @Override
    public JobLaunchPlan createPending(Long userId, Long projectId, JobCreateRequest request, String idempotencyKey) {
        validateProjectMember(projectId, userId);
        validateIdempotencyKey(idempotencyKey);

        if (idempotencyKey != null) {
            Optional<Job> existing = jobRepository.findByUserUserIdAndIdempotencyKey(userId, idempotencyKey);
            if (existing.isPresent()) {
                return toExistingPlan(existing.get(), projectId, request.getType());
            }
        }

        Project project = projectRepository.getReferenceById(projectId);
        User user = userRepository.getReferenceById(userId);
        return switch (request) {
            case AugmentationJobCreateRequest r -> createAugmentation(project, user, r, idempotencyKey);
            case TrainingJobCreateRequest r -> createTraining(project, user, r, idempotencyKey);
            case InferenceJobCreateRequest r -> createInference(project, user, r, idempotencyKey);
            default -> throw new BusinessException(ErrorCode.INVALID_REQUEST);
        };
    }

    @Override
    public void markStarted(Long jobId, String mlJobId) {
        getJob(jobId).start(mlJobId);
    }

    @Override
    public void markFailed(Long jobId, JobFailureReason reason) {
        // 입력 파일은 되돌리지 않고 Job에 연결된 채로 보존한다 (스케줄러의 실패·취소 처리와 동일, 재시도 API 대비).
        getJob(jobId).fail(reason, null);
    }

    private JobLaunchPlan createAugmentation(Project project, User user, AugmentationJobCreateRequest request,
                                             String idempotencyKey) {
        AugmentationParameters parameters = request.toParameters()
                .withDefaults(mlProperties.defaults().augmentation());
        jobRequestValidator.validateAugmentation(parameters, request.getNormalFileIds(), request.getAnomalyFileIds());
        List<File> inputFiles = getAttachableFiles(user.getUserId(),
                Stream.concat(request.getNormalFileIds().stream(), request.getAnomalyFileIds().stream()).toList(),
                false);

        Job job = jobRepository.saveAndFlush(
                Job.create(project, user, JobType.AUGMENTATION, parameters.seed(), idempotencyKey));
        augmentationConfigRepository.save(AugmentationConfig.create(
                job, parameters, request.getNormalFileIds(), request.getAnomalyFileIds()));
        inputFiles.forEach(file -> file.attachToJob(job.getJobId()));

        return JobLaunchPlan.augmentation(job.getJobId(), parameters,
                request.getNormalFileIds(), request.getAnomalyFileIds());
    }

    private JobLaunchPlan createTraining(Project project, User user, TrainingJobCreateRequest request,
                                         String idempotencyKey) {
        TrainingParameters parameters = request.toParameters()
                .withDefaults(mlProperties.defaults().training());
        jobRequestValidator.validateTraining(parameters);
        AugmentationConfig source = getReadyDataset(project.getProjectId(), request.getDatasetId());

        Job job = jobRepository.saveAndFlush(
                Job.create(project, user, JobType.TRAINING, parameters.seed(), idempotencyKey));
        trainingConfigRepository.save(TrainingConfig.create(job, source, parameters));

        return JobLaunchPlan.training(job.getJobId(), parameters, source.getMlDatasetId());
    }

    private JobLaunchPlan createInference(Project project, User user, InferenceJobCreateRequest request,
                                          String idempotencyKey) {
        jobRequestValidator.validateInference(request.getFileIds());
        TrainedModel model = trainedModelRepository.findById(request.getModelId())
                .filter(m -> m.getProject().getProjectId().equals(project.getProjectId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.TRAINED_MODEL_NOT_FOUND));
        List<File> inputFiles = getAttachableFiles(user.getUserId(), request.getFileIds(), false);

        Job job = jobRepository.saveAndFlush(
                Job.create(project, user, JobType.INFERENCE, null, idempotencyKey));
        inferenceConfigRepository.save(InferenceConfig.create(job, model, request.getFileIds()));
        inputFiles.forEach(file -> file.attachToJob(job.getJobId()));

        return JobLaunchPlan.inference(job.getJobId(), model.getMlModelId(), request.getFileIds());
    }

    /**
     * 같은 프로젝트의 SUCCESS 증강 Job이고 ML 데이터셋 ID가 있어야 학습에 쓸 수 있다.
     */
    private AugmentationConfig getReadyDataset(Long projectId, Long augmentationJobId) {
        AugmentationConfig source = augmentationConfigRepository.findByJobJobId(augmentationJobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATASET_NOT_READY));
        Job sourceJob = source.getJob();
        if (!sourceJob.getProject().getProjectId().equals(projectId)
                || sourceJob.getStatus() != JobStatus.SUCCESS
                || source.getMlDatasetId() == null) {
            throw new BusinessException(ErrorCode.DATASET_NOT_READY);
        }
        return source;
    }

    /**
     * 입력 파일 검증: 존재 → 요청자 소유 → 임시 파일(다른 Job에 연결되지 않음) → 선언 용량 합계.
     * 행 잠금으로 같은 임시 파일이 동시에 두 Job에 연결되지 않게 한다.
     *
     * @param allowReuseFromEndedJob true면 FAILED·CANCELLED Job에 연결된 본인 파일도 허용한다.
     *                               재시도 API용 조건이며, Job 생성 API는 false로 호출한다 (재사용 시 FILE_ALREADY_USED).
     */
    private List<File> getAttachableFiles(Long userId, List<Long> fileIds, boolean allowReuseFromEndedJob) {
        List<File> files = fileRepository.findAllByIdForUpdate(fileIds);
        if (files.size() != fileIds.size()) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        long totalSize = 0;
        for (File file : files) {
            if (!file.getUser().getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.FILE_ACCESS_DENIED);
            }
            if (!Boolean.TRUE.equals(file.getIsTemp())
                    && !(allowReuseFromEndedJob && isInputOfEndedJob(file))) {
                throw new BusinessException(ErrorCode.FILE_ALREADY_USED);
            }
            totalSize += file.getFileSize() != null ? file.getFileSize() : 0;
        }
        if (totalSize > mlProperties.maxInputSize().toBytes()) {
            throw new BusinessException(ErrorCode.FILE_TOTAL_SIZE_EXCEEDED);
        }
        return files;
    }

    /**
     * FAILED·CANCELLED Job의 입력 파일인지. (소유자 검증은 호출 측에서 이미 했다)
     * 결과 파일(JOB_RESULT)이나 진행 중·성공한 Job의 입력 파일은 해당하지 않는다.
     */
    private boolean isInputOfEndedJob(File file) {
        if (!File.TARGET_TYPE_JOB.equals(file.getTargetType()) || file.getTargetId() == null) {
            return false;
        }
        return jobRepository.findById(file.getTargetId())
                .map(job -> job.getStatus() == JobStatus.FAILED || job.getStatus() == JobStatus.CANCELLED)
                .orElse(false);
    }

    /**
     * 같은 키로 이미 만든 Job이 있으면 그대로 돌려준다. 다른 프로젝트·작업 종류에 같은 키를 쓰면 거절한다.
     */
    private JobLaunchPlan toExistingPlan(Job job, Long projectId, JobType type) {
        if (!job.getProject().getProjectId().equals(projectId) || job.getType() != type) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
        }
        return JobLaunchPlan.existing(job.getJobId(), job.getType());
    }

    private void validateProjectMember(Long projectId, Long userId) {
        if (!projectMemberRepository.existsByProjectProjectIdAndUserUserId(projectId, userId)) {
            throw new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED);
        }
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey != null && idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private Job getJob(Long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_NOT_FOUND));
    }
}
