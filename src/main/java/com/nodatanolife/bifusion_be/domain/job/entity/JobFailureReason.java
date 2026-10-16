package com.nodatanolife.bifusion_be.domain.job.entity;

import lombok.Getter;

import java.util.List;

/**
 * Job 실패 시 사용자에게 노출하는 문구.
 * ML error 원문에는 내부 경로·traceback·모듈명이 섞일 수 있어 FE에는 이 enum의 문구만 내보낸다.
 * (ML 원문은 Job.mlErrorDetail에만 저장하고 어떤 응답에도 포함하지 않는다.)
 *
 * 판정 순서: ML error_code → ML error 시작 문구(입력 오류만) → UNKNOWN
 */
@Getter
public enum JobFailureReason {

    NORMAL_IMAGE_INVALID(null, List.of("No valid Normal images"), List.of(),
            "정상 이미지를 읽을 수 없습니다. JPG 또는 PNG 이미지인지 확인해 주세요."),
    ANOMALY_IMAGE_INVALID(null, List.of("No valid Anomaly images"), List.of(),
            "비정상 이미지를 읽을 수 없습니다. JPG 또는 PNG 이미지인지 확인해 주세요."),
    INFERENCE_IMAGE_INVALID(null, List.of("No valid input images to diagnose"), List.of(),
            "분류할 이미지를 읽을 수 없습니다. JPG 또는 PNG 이미지인지 확인해 주세요."),
    INVALID_IMAGE("INVALID_IMAGE", List.of(), List.of(),
            "이미지를 읽을 수 없습니다. JPG 또는 PNG 이미지인지 확인해 주세요."),
    // "train_few_shot: class 0 has no samples in dataset"처럼 앞에 함수명이 붙어 오는 문구는 포함 여부로 판정한다.
    DATASET_UNAVAILABLE("DATASET_NOT_FOUND", List.of("Dataset not found"), List.of("has no samples in dataset"),
            "선택한 증강 데이터를 사용할 수 없습니다. 증강 작업을 다시 실행해 주세요."),
    MODEL_UNAVAILABLE("MODEL_NOT_FOUND", List.of("Trained ProtoNet model not found"), List.of(),
            "선택한 모델을 사용할 수 없습니다. 학습 작업을 다시 실행해 주세요."),
    GPU_OUT_OF_MEMORY("GPU_OUT_OF_MEMORY", List.of(), List.of(),
            "AI 서버의 메모리가 부족해 작업을 완료하지 못했습니다. 이미지 수를 줄이거나 잠시 후 다시 시도해 주세요."),
    ML_JOB_NOT_FOUND(null, List.of(), List.of(),
            "AI 서버에서 작업 정보를 찾을 수 없어 중단되었습니다. 작업을 다시 실행해 주세요."),
    PENDING_TIMEOUT(null, List.of(), List.of(),
            "대기 시간이 너무 길어 작업이 중단되었습니다. 잠시 후 다시 실행해 주세요."),
    UNKNOWN(null, List.of(), List.of(),
            "작업 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");

    private final String mlErrorCode;
    private final List<String> errorPrefixes;
    private final List<String> errorFragments;
    private final String message;

    JobFailureReason(String mlErrorCode, List<String> errorPrefixes, List<String> errorFragments, String message) {
        this.mlErrorCode = mlErrorCode;
        this.errorPrefixes = errorPrefixes;
        this.errorFragments = errorFragments;
        this.message = message;
    }

    /**
     * ML의 FAILED 응답(error_code, error)으로 사용자용 사유를 결정한다. 판정만 하며 로그는 남기지 않는다.
     * 입력 오류로 분류되지 않는 셋업·GPU·인프라 오류는 UNKNOWN.
     */
    public static JobFailureReason resolve(String mlErrorCode, String mlError) {
        if (mlErrorCode != null) {
            for (JobFailureReason reason : values()) {
                if (mlErrorCode.equals(reason.mlErrorCode)) {
                    return reason;
                }
            }
        }
        if (mlError != null) {
            String error = mlError.strip();
            for (JobFailureReason reason : values()) {
                if (reason.errorPrefixes.stream().anyMatch(error::startsWith)
                        || reason.errorFragments.stream().anyMatch(error::contains)) {
                    return reason;
                }
            }
        }
        return UNKNOWN;
    }
}
