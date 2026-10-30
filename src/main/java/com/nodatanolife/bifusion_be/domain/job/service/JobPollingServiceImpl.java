package com.nodatanolife.bifusion_be.domain.job.service;

import com.nodatanolife.bifusion_be.domain.file.entity.File;
import com.nodatanolife.bifusion_be.domain.file.repository.FileRepository;
import com.nodatanolife.bifusion_be.domain.file.service.S3ObjectCleaner;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationConfig;
import com.nodatanolife.bifusion_be.domain.job.entity.InferenceConfig;
import com.nodatanolife.bifusion_be.domain.job.entity.InferenceResult;
import com.nodatanolife.bifusion_be.domain.job.entity.Job;
import com.nodatanolife.bifusion_be.domain.job.entity.JobFailureReason;
import com.nodatanolife.bifusion_be.domain.job.entity.JobStatus;
import com.nodatanolife.bifusion_be.domain.job.entity.TrainedModel;
import com.nodatanolife.bifusion_be.domain.job.entity.TrainingConfig;
import com.nodatanolife.bifusion_be.domain.job.ml.MlProperties;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.response.MlAugmentationResult;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.response.MlInferenceResult;
import com.nodatanolife.bifusion_be.domain.job.ml.dto.response.MlTrainingResult;
import com.nodatanolife.bifusion_be.domain.job.repository.AugmentationConfigRepository;
import com.nodatanolife.bifusion_be.domain.job.repository.InferenceConfigRepository;
import com.nodatanolife.bifusion_be.domain.job.repository.InferenceResultRepository;
import com.nodatanolife.bifusion_be.domain.job.repository.JobRepository;
import com.nodatanolife.bifusion_be.domain.job.repository.TrainedModelRepository;
import com.nodatanolife.bifusion_be.domain.job.repository.TrainingConfigRepository;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class JobPollingServiceImpl implements JobPollingService {

    private static final List<JobStatus> POLL_STATUSES = List.of(JobStatus.PENDING, JobStatus.RUNNING);

    private final JobRepository jobRepository;
    private final AugmentationConfigRepository augmentationConfigRepository;
    private final TrainingConfigRepository trainingConfigRepository;
    private final InferenceConfigRepository inferenceConfigRepository;
    private final InferenceResultRepository inferenceResultRepository;
    private final TrainedModelRepository trainedModelRepository;
    private final FileRepository fileRepository;
    private final MlProperties mlProperties;
    private final JobResultNotifier jobResultNotifier;
    private final JobPointService jobPointService;
    private final S3ObjectCleaner s3ObjectCleaner;

    @Override
    @Transactional(readOnly = true)
    public List<Job> findPollTargets() {
        return jobRepository.findPollTargets(POLL_STATUSES, PageRequest.of(0, mlProperties.polling().batchSize()));
    }

    @Override
    public boolean fail(Long jobId, JobFailureReason reason, String mlErrorDetail) {
        Optional<Job> active = findActiveJob(jobId);
        active.ifPresent(job -> {
            job.fail(reason, mlErrorDetail);
            jobPointService.refund(job);
            jobResultNotifier.notifyFailed(job);
        });
        return active.isPresent();
    }

    @Override
    public void applyProgress(Long jobId, boolean running, String currentStep) {
        findActiveJob(jobId).ifPresent(job -> {
            if (running && job.getStatus() == JobStatus.PENDING) {
                job.markRunning();
            }
            if (currentStep != null) {
                job.updateCurrentStep(currentStep);
            }
        });
    }

    @Override
    public void cancel(Long jobId) {
        findActiveJob(jobId).ifPresent(job -> {
            job.cancel();
            jobPointService.refund(job);
        });
    }

    @Override
    public void applyAugmentationSuccess(Long jobId, MlAugmentationResult result, ResultFileMeta dataset, ResultFileMeta preview,
                                         ResultFileMeta downloadZip) {
        // 결과로 기록하지 못하면(이미 끝난 Job, 잘못된 응답, 롤백) 미리 올린 zip이 남지 않게 지운다
        List<String> zipKeys = Stream.ofNullable(ResultFileMeta.keyOf(downloadZip)).toList();
        s3ObjectCleaner.deleteAfterRollback(zipKeys);
        Optional<Job> active = findActiveJob(jobId);
        if (active.isEmpty()) {
            s3ObjectCleaner.deleteAfterCommit(zipKeys);
            return;
        }
        Job job = active.get();
        if (failIfInvalid(job, result.invalidReason())) {
            s3ObjectCleaner.deleteAfterCommit(zipKeys);
            return;
        }
        AugmentationConfig config = augmentationConfigRepository.findByJobJobId(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_NOT_FOUND));

        job.succeed();
        jobResultNotifier.notifySucceeded(job);
        config.recordResult(
                result.datasetId(),
                ResultFileMeta.keyOf(dataset),
                ResultFileMeta.keyOf(preview),
                result.numSyntheticGenerated(),
                result.totalSamples()
        );
        config.recordDownloadZip(ResultFileMeta.keyOf(downloadZip));
        saveResultFiles(job, dataset, preview, downloadZip);
    }

    @Override
    public boolean recordDownloadZip(Long jobId, ResultFileMeta downloadZip) {
        AugmentationConfig config = augmentationConfigRepository.findByJobIdForUpdate(jobId).orElse(null);
        if (config == null || !config.canCreateDownloadZip()) {
            s3ObjectCleaner.deleteAfterCommit(List.of(downloadZip.s3Key()));
            return false;
        }
        if (config.getDownloadZipKey() != null) {
            return true;
        }
        config.recordDownloadZip(downloadZip.s3Key());
        saveResultFiles(config.getJob(), downloadZip);
        return true;
    }

    @Override
    public void applyTrainingSuccess(Long jobId, MlTrainingResult result, ResultFileMeta model) {
        Optional<Job> active = findActiveJob(jobId);
        if (active.isEmpty()) {
            return;
        }
        Job job = active.get();
        if (failIfInvalid(job, result.invalidReason())) {
            return;
        }
        TrainingConfig config = trainingConfigRepository.findByJobJobId(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_NOT_FOUND));

        job.succeed();
        jobResultNotifier.notifySucceeded(job);
        MlTrainingResult.Metrics metrics = result.metrics();
        if (metrics != null) {
            config.recordMetrics(metrics.accuracy(), metrics.f1Score(), metrics.auroc(), metrics.loss());
        }
        // k-shot은 ML 결과값이 아니라 BE가 실제로 보낸 파라미터를 기준으로 기록한다.
        trainedModelRepository.save(TrainedModel.create(
                job, result.modelId(), config.getKShot(), ResultFileMeta.keyOf(model)));
        saveResultFiles(job, model);
    }

    @Override
    public void applyInferenceSuccess(Long jobId, MlInferenceResult result) {
        Optional<Job> active = findActiveJob(jobId);
        if (active.isEmpty()) {
            return;
        }
        Job job = active.get();
        InferenceConfig config = inferenceConfigRepository.findByJobJobId(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_NOT_FOUND));
        List<Long> inputFileIds = config.getInputFileIds();
        if (failIfInvalid(job, result.invalidReason(inputFileIds.size()))) {
            return;
        }
        Map<Long, File> fileById = fileRepository.findAllById(inputFileIds).stream()
                .collect(Collectors.toMap(File::getFileId, Function.identity()));
        if (fileById.size() != inputFileIds.size()) {
            failIfInvalid(job, "입력 파일 " + (inputFileIds.size() - fileById.size()) + "건을 찾을 수 없음");
            return;
        }

        job.succeed();
        jobResultNotifier.notifySucceeded(job);
        // sample_index는 ML 요청에 보낸 이미지 순서(= inputFileIds 순서)다.
        List<InferenceResult> results = result.predictions().stream()
                .map(prediction -> InferenceResult.create(
                        config,
                        fileById.get(inputFileIds.get(prediction.sampleIndex())),
                        prediction.sampleIndex(),
                        prediction.predictedClass(),
                        prediction.confidence(),
                        prediction.probabilities() != null ? prediction.probabilities().normal() : null,
                        prediction.probabilities() != null ? prediction.probabilities().anomaly() : null))
                .toList();
        inferenceResultRepository.saveAll(results);
        MlInferenceResult.Summary summary = result.summary();
        config.recordSummary(summary.normalCount(), summary.anomalyCount(), summary.avgConfidence());
    }

    /**
     * ML이 SUCCESS라고 했지만 저장할 수 없는 결과면 가짜 성공으로 남기지 않고 실패 처리한다.
     * mlErrorDetail에는 응답 본문 대신 판정 사유만 남긴다.
     */
    private boolean failIfInvalid(Job job, String invalidReason) {
        if (invalidReason == null) {
            return false;
        }
        log.error("[Polling] ML SUCCESS 응답을 결과로 저장할 수 없음 - jobId: {}, mlJobId: {}, 사유: {}",
                job.getJobId(), job.getMlJobId(), invalidReason);
        job.fail(JobFailureReason.UNKNOWN, invalidReason);
        jobPointService.refund(job);
        jobResultNotifier.notifyFailed(job);
        return true;
    }

    private void saveResultFiles(Job job, ResultFileMeta... resultFiles) {
        Stream.of(resultFiles).filter(Objects::nonNull).forEach(meta -> fileRepository.save(File.createJobResult(
                job.getUser(), job.getJobId(), meta.s3Key(), meta.contentType(), meta.size())));
    }

    private Optional<Job> findActiveJob(Long jobId) {
        return jobRepository.findById(jobId)
                .filter(job -> !job.getStatus().isTerminal());
    }
}
