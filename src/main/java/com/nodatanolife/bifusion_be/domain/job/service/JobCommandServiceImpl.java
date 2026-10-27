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
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectMember;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectRole;
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
    private final JobResultNotifier jobResultNotifier;
    private final JobCostPolicy jobCostPolicy;
    private final JobPointService jobPointService;

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

    /**
     * 재시도 트랜잭션 1. 원본 Job을 행 잠금으로 읽어 동시 재시도를 막고,
     * 원본의 파라미터 스냅샷(seed 포함)·입력 파일 순서·학습/추론 참조를 그대로 복사해 새 Job을 만든다.
     * 원본 쪽 재시도 기록(retriedByJobId)과 새 Job의 원본 참조(retryOfJob)는 이 트랜잭션에서 함께 설정한다.
     */
    @Override
    public JobLaunchPlan createRetry(Long userId, Long sourceJobId, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        Job source = jobRepository.findByIdForUpdate(sourceJobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_NOT_FOUND));
        validateProjectMember(source.getProject().getProjectId(), userId);

        if (idempotencyKey != null) {
            Optional<Job> existing = jobRepository.findByUserUserIdAndIdempotencyKey(userId, idempotencyKey);
            if (existing.isPresent()) {
                return toExistingRetryPlan(existing.get(), sourceJobId);
            }
        }
        source.validateRetryable();

        Job retryJob = Job.createRetry(source, userRepository.getReferenceById(userId), idempotencyKey);
        JobLaunchPlan plan = switch (source.getType()) {
            case AUGMENTATION -> retryAugmentation(source, retryJob);
            case TRAINING -> retryTraining(source, retryJob);
            case INFERENCE -> retryInference(source, retryJob);
        };
        source.markRetriedBy(retryJob);
        return plan.asRetry();
    }

    @Override
    @Transactional(readOnly = true)
    public JobCancelPlan prepareCancel(Long userId, Long jobId) {
        Job job = getJob(jobId);
        ProjectMember member = projectMemberRepository
                .findByProjectProjectIdAndUserUserId(job.getProject().getProjectId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED));
        // 취소는 다른 사람이 진행 중인 작업을 멈추는 동작이라, 재시도(멤버 누구나)와 달리 본인·관리자로 제한한다.
        if (!job.isOwnedBy(userId) && member.getRole() != ProjectRole.MANAGER) {
            throw new BusinessException(ErrorCode.JOB_CANCEL_FORBIDDEN);
        }
        job.validateCancellable();
        return new JobCancelPlan(job.getJobId(), job.getType(), job.getMlJobId(),
                job.getStatus() == JobStatus.CANCELLED);
    }

    @Override
    public void applyCancel(Long jobId) {
        Job job = getJob(jobId);
        if (job.getStatus() == JobStatus.CANCELLED) {
            return;
        }
        // 폴러가 먼저 SUCCESS·FAILED로 반영했으면 cancel()이 JOB_ALREADY_FINISHED를 던진다.
        job.cancel();
        jobPointService.refund(job);
    }

    @Override
    public void markStarted(Long jobId, String mlJobId) {
        getJob(jobId).start(mlJobId);
    }

    @Override
    public void markFailed(Long jobId, JobFailureReason reason) {
        fail(jobId, reason);
    }

    @Override
    public void markFailedAndNotify(Long jobId, JobFailureReason reason) {
        jobResultNotifier.notifyFailed(fail(jobId, reason));
    }

    // 입력 파일은 되돌리지 않고 Job에 연결된 채로 보존한다 (스케줄러의 실패·취소 처리와 동일, 재시도 API 대비).
    private Job fail(Long jobId, JobFailureReason reason) {
        Job job = getJob(jobId);
        job.fail(reason, null);
        jobPointService.refund(job);
        return job;
    }

    private JobLaunchPlan createAugmentation(Project project, User user, AugmentationJobCreateRequest request,
                                             String idempotencyKey) {
        AugmentationParameters parameters = request.toParameters()
                .withDefaults(mlProperties.defaults().augmentation());
        jobRequestValidator.validateAugmentation(parameters, request.getNormalFileIds(), request.getAnomalyFileIds());
        List<File> inputFiles = getNewInputFiles(user.getUserId(),
                Stream.concat(request.getNormalFileIds().stream(), request.getAnomalyFileIds().stream()).toList());

        return saveAugmentation(
                Job.create(project, user, JobType.AUGMENTATION, parameters.seed(), idempotencyKey),
                parameters, request.getNormalFileIds(), request.getAnomalyFileIds(), inputFiles);
    }

    private JobLaunchPlan createTraining(Project project, User user, TrainingJobCreateRequest request,
                                         String idempotencyKey) {
        TrainingParameters parameters = request.toParameters()
                .withDefaults(mlProperties.defaults().training());
        jobRequestValidator.validateTraining(parameters);
        AugmentationConfig source = getReadyDataset(project.getProjectId(), request.getDatasetId());

        return saveTraining(
                Job.create(project, user, JobType.TRAINING, parameters.seed(), idempotencyKey),
                source, parameters);
    }

    private JobLaunchPlan createInference(Project project, User user, InferenceJobCreateRequest request,
                                          String idempotencyKey) {
        jobRequestValidator.validateInference(request.getFileIds());
        TrainedModel model = trainedModelRepository.findById(request.getModelId())
                .filter(m -> m.getProject().getProjectId().equals(project.getProjectId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.TRAINED_MODEL_NOT_FOUND));
        List<File> inputFiles = getNewInputFiles(user.getUserId(), request.getFileIds());

        return saveInference(
                Job.create(project, user, JobType.INFERENCE, null, idempotencyKey),
                model, request.getFileIds(), inputFiles);
    }

    private JobLaunchPlan retryAugmentation(Job source, Job retryJob) {
        AugmentationConfig config = augmentationConfigRepository.findByJobJobId(source.getJobId())
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_NOT_FOUND));
        if (config.getParameters() == null || config.getNormalFileIds() == null || config.getAnomalyFileIds() == null) {
            // 실행 기록(파라미터 스냅샷·입력 파일 목록)이 없는 예전 Job은 같은 입력으로 다시 실행할 수 없다.
            throw new BusinessException(ErrorCode.JOB_RETRY_NEEDS_NEW_INPUT);
        }
        List<File> inputFiles = getRetryInputFiles(source.getJobId(),
                Stream.concat(config.getNormalFileIds().stream(), config.getAnomalyFileIds().stream()).toList());
        return saveAugmentation(retryJob, config.getParameters(),
                config.getNormalFileIds(), config.getAnomalyFileIds(), inputFiles);
    }

    private JobLaunchPlan retryTraining(Job source, Job retryJob) {
        TrainingConfig config = trainingConfigRepository.findByJobJobId(source.getJobId())
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_NOT_FOUND));
        if (config.getParameters() == null) {
            throw new BusinessException(ErrorCode.JOB_RETRY_NEEDS_NEW_INPUT);
        }
        // 원본이 쓰던 증강 데이터가 여전히 학습에 쓸 수 있어야 한다 (생성 API와 같은 기준).
        if (!config.getAugmentationConfig().isReadyForTraining()) {
            throw new BusinessException(ErrorCode.DATASET_NOT_READY);
        }
        return saveTraining(retryJob, config.getAugmentationConfig(), config.getParameters());
    }

    private JobLaunchPlan retryInference(Job source, Job retryJob) {
        InferenceConfig config = inferenceConfigRepository.findByJobJobId(source.getJobId())
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_NOT_FOUND));
        if (config.getInputFileIds() == null) {
            throw new BusinessException(ErrorCode.JOB_RETRY_NEEDS_NEW_INPUT);
        }
        TrainedModel model = config.getTrainedModel();
        if (model == null) {
            throw new BusinessException(ErrorCode.TRAINED_MODEL_NOT_FOUND);
        }
        List<File> inputFiles = getRetryInputFiles(source.getJobId(), config.getInputFileIds());
        return saveInference(retryJob, model, config.getInputFileIds(), inputFiles);
    }

    /*
     * 아래 save* 메서드는 검증이 끝난 값으로 Job·Config를 저장하고 입력 파일을 연결한 뒤, ML 실행 계획을 돌려준다.
     * 값을 어디서 만드는지(요청 DTO 또는 원본 Job)와 무관하게 같은 저장 로직을 쓰기 위해 분리했다.
     * saveAndFlush: 멱등 키 unique 위반을 이 트랜잭션 안에서 바로 드러내기 위해.
     * 실행 비용은 저장한 파라미터로 계산해 여기서 차감한다. 잔액이 모자라면 Job·Config 저장까지 롤백된다.
     */

    private JobLaunchPlan saveAugmentation(Job job, AugmentationParameters parameters,
                                           List<Long> normalFileIds, List<Long> anomalyFileIds,
                                           List<File> inputFiles) {
        Job saved = jobRepository.saveAndFlush(job);
        jobPointService.charge(saved, jobCostPolicy.augmentation(parameters));
        augmentationConfigRepository.save(AugmentationConfig.create(saved, parameters, normalFileIds, anomalyFileIds));
        inputFiles.forEach(file -> file.attachToJob(saved.getJobId()));
        return JobLaunchPlan.augmentation(saved.getJobId(), parameters, normalFileIds, anomalyFileIds);
    }

    private JobLaunchPlan saveTraining(Job job, AugmentationConfig source, TrainingParameters parameters) {
        Job saved = jobRepository.saveAndFlush(job);
        jobPointService.charge(saved, jobCostPolicy.training(parameters));
        trainingConfigRepository.save(TrainingConfig.create(saved, source, parameters));
        return JobLaunchPlan.training(saved.getJobId(), parameters, source.getMlDatasetId());
    }

    private JobLaunchPlan saveInference(Job job, TrainedModel model, List<Long> inputFileIds, List<File> inputFiles) {
        Job saved = jobRepository.saveAndFlush(job);
        jobPointService.charge(saved, jobCostPolicy.inference(inputFileIds.size()));
        inferenceConfigRepository.save(InferenceConfig.create(saved, model, inputFileIds));
        inputFiles.forEach(file -> file.attachToJob(saved.getJobId()));
        return JobLaunchPlan.inference(saved.getJobId(), model.getMlModelId(), inputFileIds);
    }

    /**
     * 같은 프로젝트의 증강 결과이고 학습에 쓸 수 있어야 한다.
     * 판정 기준은 증강 데이터셋 목록과 같은 AugmentationConfig.isReadyForTraining()을 쓴다.
     */
    private AugmentationConfig getReadyDataset(Long projectId, Long augmentationJobId) {
        AugmentationConfig source = augmentationConfigRepository.findByJobJobId(augmentationJobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATASET_NOT_READY));
        if (!source.getJob().getProject().getProjectId().equals(projectId) || !source.isReadyForTraining()) {
            throw new BusinessException(ErrorCode.DATASET_NOT_READY);
        }
        return source;
    }

    /**
     * Job 생성용 입력 파일 검증: 존재 → 요청자 소유 → 임시 파일(다른 Job에 연결되지 않음) → 선언 용량 합계.
     */
    private List<File> getNewInputFiles(Long userId, List<Long> fileIds) {
        List<File> files = lockFiles(fileIds);
        for (File file : files) {
            if (!file.getUser().getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.FILE_ACCESS_DENIED);
            }
            if (!Boolean.TRUE.equals(file.getIsTemp())) {
                throw new BusinessException(ErrorCode.FILE_ALREADY_USED);
            }
        }
        validateTotalSize(files);
        return files;
    }

    /**
     * 재시도용 입력 파일 검증: 존재 → 재시도 대상 Job의 입력 파일(JOB 타입으로 연결) → 선언 용량 합계.
     * 재시도는 프로젝트 멤버 누구나 할 수 있어, 파일 소유자는 비교하지 않는다.
     * (재시도 대상 Job이 FAILED·CANCELLED인지는 Job.validateRetryable()에서 이미 확인했다)
     */
    private List<File> getRetryInputFiles(Long sourceJobId, List<Long> fileIds) {
        List<File> files = lockFiles(fileIds);
        for (File file : files) {
            if (!File.TARGET_TYPE_JOB.equals(file.getTargetType()) || !sourceJobId.equals(file.getTargetId())) {
                throw new BusinessException(ErrorCode.FILE_ALREADY_USED);
            }
        }
        validateTotalSize(files);
        return files;
    }

    /**
     * 행 잠금으로 같은 파일이 동시에 두 Job에 연결되지 않게 한다.
     */
    private List<File> lockFiles(List<Long> fileIds) {
        List<File> files = fileRepository.findAllByIdForUpdate(fileIds);
        if (files.size() != fileIds.size()) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        return files;
    }

    private void validateTotalSize(List<File> files) {
        long totalSize = files.stream()
                .mapToLong(file -> file.getFileSize() != null ? file.getFileSize() : 0)
                .sum();
        if (totalSize > mlProperties.maxInputSize().toBytes()) {
            throw new BusinessException(ErrorCode.FILE_TOTAL_SIZE_EXCEEDED);
        }
    }

    /**
     * 같은 키로 이미 만든 Job이 있으면 그대로 돌려준다. 다른 프로젝트·작업 종류에 같은 키를 쓰면 거절한다.
     * 실행 요청 단계에서 실패한 Job이면 첫 요청과 같은 에러로 응답한다 (rejectIfSubmitFailed).
     */
    private JobLaunchPlan toExistingPlan(Job job, Long projectId, JobType type) {
        if (!job.getProject().getProjectId().equals(projectId) || job.getType() != type) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
        }
        rejectIfSubmitFailed(job);
        return JobLaunchPlan.existing(job.getJobId(), job.getType());
    }

    /**
     * 같은 키로 이미 만든 재시도 Job이 있으면 그대로 돌려준다. 다른 Job의 재시도(또는 생성)에 쓴 키면 거절한다.
     */
    private JobLaunchPlan toExistingRetryPlan(Job job, Long sourceJobId) {
        if (!sourceJobId.equals(job.getRetryOfJobId())) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
        }
        rejectIfSubmitFailed(job);
        return JobLaunchPlan.existing(job.getJobId(), job.getType());
    }

    /**
     * 첫 요청이 ML 접수 실패로 에러 응답했으면 같은 키의 재요청에도 같은 에러로 응답한다.
     * 차감·환불은 첫 요청에서 끝났으므로 여기서는 아무것도 바꾸지 않는다.
     */
    private static void rejectIfSubmitFailed(Job job) {
        ErrorCode errorCode = job.submitFailureErrorCode();
        if (errorCode != null) {
            throw new BusinessException(errorCode);
        }
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
