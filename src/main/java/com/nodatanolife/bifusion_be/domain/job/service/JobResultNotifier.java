package com.nodatanolife.bifusion_be.domain.job.service;

import com.nodatanolife.bifusion_be.domain.job.entity.Job;
import com.nodatanolife.bifusion_be.domain.notification.entity.NotificationType;
import com.nodatanolife.bifusion_be.domain.notification.entity.TargetType;
import com.nodatanolife.bifusion_be.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Job(증강·학습·추론)이 성공·실패로 끝났을 때 실행한 사용자에게 알린다.
 * 상태를 바꾼 트랜잭션 안에서 호출해, 상태 반영이 롤백되면 알림도 남지 않게 한다.
 * 사용자가 직접 한 취소, 실행 요청의 에러 응답으로 이미 전달된 실패는 알리지 않는다.
 * 실행자가 탈퇴해 공용 탈퇴 계정으로 바뀐 Job은 NotificationService에서 걸러진다.
 */
@Component
@RequiredArgsConstructor
public class JobResultNotifier {

    private final NotificationService notificationService;

    public void notifySucceeded(Job job) {
        NotificationType type = switch (job.getType()) {
            case AUGMENTATION -> NotificationType.AUGMENTATION_SUCCESS;
            case TRAINING -> NotificationType.TRAINING_SUCCESS;
            case INFERENCE -> NotificationType.INFERENCE_SUCCESS;
        };
        String jobName = jobNameOf(job);
        notificationService.create(
                job.getUser(),
                type,
                jobName + " 완료",
                "\"" + job.getProject().getTitle() + "\" 프로젝트의 " + jobName + " 작업(" + job.getDisplayCode() + ")이 완료되었습니다.",
                TargetType.JOB,
                job.getJobId()
        );
    }

    /**
     * 실패 사유는 사용자용 문구(Job.errorMessage)만 쓴다. ML 원문(mlErrorDetail)은 알림에도 넣지 않는다.
     */
    public void notifyFailed(Job job) {
        NotificationType type = switch (job.getType()) {
            case AUGMENTATION -> NotificationType.AUGMENTATION_FAILED;
            case TRAINING -> NotificationType.TRAINING_FAILED;
            case INFERENCE -> NotificationType.INFERENCE_FAILED;
        };
        String jobName = jobNameOf(job);
        notificationService.create(
                job.getUser(),
                type,
                jobName + " 실패",
                "\"" + job.getProject().getTitle() + "\" 프로젝트의 " + jobName + " 작업(" + job.getDisplayCode() + ")이 실패했습니다. "
                        + job.getErrorMessage(),
                TargetType.JOB,
                job.getJobId()
        );
    }

    private String jobNameOf(Job job) {
        return switch (job.getType()) {
            case AUGMENTATION -> "데이터 증강";
            case TRAINING -> "모델 학습";
            case INFERENCE -> "이미지 추론";
        };
    }
}
