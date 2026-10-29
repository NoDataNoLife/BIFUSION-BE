package com.nodatanolife.bifusion_be.domain.job.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.nodatanolife.bifusion_be.domain.file.entity.File;
import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationConfig;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationParameters;
import com.nodatanolife.bifusion_be.domain.job.entity.InferenceConfig;
import com.nodatanolife.bifusion_be.domain.job.entity.Job;
import com.nodatanolife.bifusion_be.domain.job.entity.JobStatus;
import com.nodatanolife.bifusion_be.domain.job.entity.JobType;
import com.nodatanolife.bifusion_be.domain.job.entity.TrainingConfig;
import com.nodatanolife.bifusion_be.domain.job.entity.TrainingParameters;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AI Job 생성·조회 응답. FE parseJob 형식을 따른다.
 * error는 사용자용 문구(errorMessage)만 담고, ML 원문(mlErrorDetail)은 절대 포함하지 않는다.
 * retryable이 true일 때만 FE가 "다시 시도"를 보여준다. retryOfJobId는 이 Job이 재시도한 원본 jobId(없으면 null).
 * 실행 입력값(numSamplesPerClass ~ inputImages)은 Config에 저장된, 실제 ML에 보낸 값이다.
 * 해당 종류가 아닌 필드와 스냅샷이 없는 예전 Job의 값은 기본값으로 채우지 않고 null로 둔다.
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
        LocalDateTime updatedAt,
        @Schema(description = "[증강] 종류별 추가 생성 장수. 증강이 아니거나 기록이 없는 예전 작업이면 null", example = "10")
        Integer numSamplesPerClass,
        @Schema(description = "[증강] 원본 변화 강도. 증강이 아니거나 기록이 없는 예전 작업이면 null", example = "0.3")
        Double strength,
        @Schema(description = "[증강] 업로드 이미지 맞춤 추가 학습 횟수. 증강이 아니거나 기록이 없는 예전 작업이면 null", example = "400")
        Integer finetuneSteps,
        @Schema(description = "[증강] 이미지 생성 계산 단계 수. 증강이 아니거나 기록이 없는 예전 작업이면 null", example = "50")
        Integer steps,
        @Schema(description = "[학습] 학습에 쓴 증강 작업 번호. 학습이 아니면 null", example = "15")
        Long augmentationJobId,
        @Schema(description = "[학습] K-shot. 학습이 아니거나 기록이 없는 예전 작업이면 null", example = "5")
        Integer kShot,
        @Schema(description = "[학습] 학습 반복 횟수. 학습이 아니거나 기록이 없는 예전 작업이면 null", example = "30")
        Integer epochs,
        @Schema(description = "[학습] 에피소드당 연습 이미지 수. 학습이 아니거나 기록이 없는 예전 작업이면 null", example = "5")
        Integer qQuery,
        @Schema(description = "[학습] 학습 반복당 에피소드 수. 학습이 아니거나 기록이 없는 예전 작업이면 null", example = "10")
        Integer episodesPerEpoch,
        @Schema(description = "[증강·학습] 실제 ML에 보낸 시드. 추론이거나 기록이 없는 예전 작업이면 null", example = "42")
        Long seed,
        @Schema(description = "[추론] 추론에 쓴 모델을 만든 학습 작업 번호. 추론이 아니면 null", example = "20")
        Long trainingJobId,
        @Schema(description = "[추론] 입력 이미지 (요청 순서). 추론이 아니거나 기록이 없는 예전 작업이면 null")
        List<InputImage> inputImages
) {

    /**
     * @param config             이 증강 Job의 Config. 없으면 실행 입력값은 모두 null
     * @param mlUnreachableSince 현재 ML 연결 불가 시작 시각 (MlConnectionState). 연결돼 있으면 null
     */
    public static JobResponse from(Job job, JobResult result, AugmentationConfig config,
                                   LocalDateTime mlUnreachableSince) {
        AugmentationParameters parameters = config != null ? config.getParameters() : null;
        if (parameters == null) {
            return of(job, result, mlUnreachableSince, null, null, null, null,
                    null, null, null, null, null, null, null, null);
        }
        return of(job, result, mlUnreachableSince,
                parameters.numSamplesPerClass(), parameters.strength(), parameters.finetuneSteps(), parameters.steps(),
                null, null, null, null, null, parameters.seed(), null, null);
    }

    /**
     * augmentationJobId는 스냅샷이 아니라 Config의 참조라 예전 Job도 값이 있다.
     */
    public static JobResponse from(Job job, JobResult result, TrainingConfig config,
                                   LocalDateTime mlUnreachableSince) {
        Long augmentationJobId = config != null ? config.getAugmentationConfig().getJob().getJobId() : null;
        TrainingParameters parameters = config != null ? config.getParameters() : null;
        if (parameters == null) {
            return of(job, result, mlUnreachableSince, null, null, null, null,
                    augmentationJobId, null, null, null, null, null, null, null);
        }
        return of(job, result, mlUnreachableSince, null, null, null, null,
                augmentationJobId, parameters.kShot(), parameters.epochs(), parameters.qQuery(),
                parameters.episodesPerEpoch(), parameters.seed(), null, null);
    }

    /**
     * @param inputFiles config.inputFileIds로 조회한 파일 (순서 무관). 행이 없어진 파일은 inputImages에서 빠진다.
     */
    public static JobResponse from(Job job, JobResult result, InferenceConfig config, List<File> inputFiles,
                                   LocalDateTime mlUnreachableSince) {
        Long trainingJobId = config != null ? config.getTrainedModel().getTrainingJob().getJobId() : null;
        List<InputImage> inputImages = config != null && config.getInputFileIds() != null
                ? InputImage.listOf(config.getInputFileIds(), inputFiles)
                : null;
        return of(job, result, mlUnreachableSince, null, null, null, null,
                null, null, null, null, null, null, trainingJobId, inputImages);
    }

    private static JobResponse of(Job job, JobResult result, LocalDateTime mlUnreachableSince,
                                  Integer numSamplesPerClass, Double strength, Integer finetuneSteps, Integer steps,
                                  Long augmentationJobId, Integer kShot, Integer epochs, Integer qQuery,
                                  Integer episodesPerEpoch, Long seed,
                                  Long trainingJobId, List<InputImage> inputImages) {
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
                job.getUpdatedAt(),
                numSamplesPerClass,
                strength,
                finetuneSteps,
                steps,
                augmentationJobId,
                kShot,
                epochs,
                qQuery,
                episodesPerEpoch,
                seed,
                trainingJobId,
                inputImages
        );
    }

    @Schema(name = "JobInputImage", description = "추론 입력 이미지")
    public record InputImage(
            @Schema(description = "이미지 fileId", example = "301")
            Long fileId,
            @Schema(description = "업로드한 파일 이름", example = "sample_01.png")
            String fileName,
            @Schema(description = "이미지 주소 (presigned, 조회할 때마다 새로 발급)")
            @PresignedUrl String url
    ) {

        public static InputImage from(File file) {
            return new InputImage(file.getFileId(), file.getFileName(), file.getS3Key());
        }

        /**
         * fileIds 순서대로 변환한다. files에 없는 id는 건너뛴다.
         */
        static List<InputImage> listOf(List<Long> fileIds, List<File> files) {
            Map<Long, File> filesById = files.stream()
                    .collect(Collectors.toMap(File::getFileId, Function.identity(), (a, b) -> a));
            return fileIds.stream()
                    .map(filesById::get)
                    .filter(Objects::nonNull)
                    .map(InputImage::from)
                    .toList();
        }
    }
}
