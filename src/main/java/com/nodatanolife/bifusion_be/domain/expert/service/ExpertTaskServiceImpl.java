package com.nodatanolife.bifusion_be.domain.expert.service;

import com.nodatanolife.bifusion_be.domain.expert.dto.response.ExpertTaskStartResponse;
import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertTask;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequestStatus;
import com.nodatanolife.bifusion_be.domain.expert.repository.ExpertTaskRepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpertTaskServiceImpl implements ExpertTaskService {

    private static final String STATUS_PENDING = "PENDING";

    private final ExpertTaskRepository expertTaskRepository;

    /**
     * 검수 시작
     * 검수전(PENDING) 상태의 요청만 시작 가능하며, 이미 진행 중이거나 완료된 요청은 409.
     * ExpertTask.status와 별개로, 연결된 InspectionRequest이 이미 APPROVED/REJECTED로
     * 종료된 경우도 같은 409로 막는다. (데이터 불일치에 대한 이중 방어)
     */
    @Override
    public ExpertTaskStartResponse startTask(User expert, Long taskId) {
        // 신청자 취소와 동시에 들어오면 한쪽만 반영되도록 task를 잠가서 조회한다.
        ExpertTask task = expertTaskRepository.findForUpdate(taskId, expert)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXPERT_TASK_NOT_FOUND));

        boolean taskNotPending = !STATUS_PENDING.equals(task.getStatus());
        boolean verificationClosed = task.getInspectionRequest().getStatus() != InspectionRequestStatus.PENDING;
        if (taskNotPending || verificationClosed) {
            throw new BusinessException(ErrorCode.EXPERT_TASK_ALREADY_STARTED_OR_COMPLETED);
        }

        task.start();

        return ExpertTaskStartResponse.from(task);
    }
}
