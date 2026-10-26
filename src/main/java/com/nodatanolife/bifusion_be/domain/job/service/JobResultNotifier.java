package com.nodatanolife.bifusion_be.domain.job.service;

import com.nodatanolife.bifusion_be.domain.job.entity.Job;
import com.nodatanolife.bifusion_be.domain.job.entity.JobType;
import com.nodatanolife.bifusion_be.domain.notification.entity.NotificationType;
import com.nodatanolife.bifusion_be.domain.notification.entity.TargetType;
import com.nodatanolife.bifusion_be.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Job이 성공·실패로 끝났을 때 실행한 사용자에게 알린다. 알림 타입이 있는 증강 Job만 대상이다.
 * 상태를 바꾼 트랜잭션 안에서 호출해, 상태 반영이 롤백되면 알림도 남지 않게 한다.
 * 사용자가 직접 한 취소는 알리지 않는다.
 */
@Component
@RequiredArgsConstructor
public class JobResultNotifier {

    private final NotificationService notificationService;

    public void notifySucceeded(Job job) {
        if (job.getType() != JobType.AUGMENTATION) {
            return;
        }
        notificationService.create(
                job.getUser(),
                NotificationType.AUGMENTATION_SUCCESS,
                "데이터 증강 완료",
                "\"" + job.getProject().getTitle() + "\" 프로젝트의 증강 작업(" + job.getJobCode() + ")이 완료되었습니다.",
                TargetType.JOB,
                job.getJobId()
        );
    }

    /**
     * 실패 사유는 사용자용 문구(Job.errorMessage)만 쓴다. ML 원문(mlErrorDetail)은 알림에도 넣지 않는다.
     */
    public void notifyFailed(Job job) {
        if (job.getType() != JobType.AUGMENTATION) {
            return;
        }
        notificationService.create(
                job.getUser(),
                NotificationType.AUGMENTATION_FAILED,
                "데이터 증강 실패",
                "\"" + job.getProject().getTitle() + "\" 프로젝트의 증강 작업(" + job.getJobCode() + ")이 실패했습니다. "
                        + job.getErrorMessage(),
                TargetType.JOB,
                job.getJobId()
        );
    }
}
