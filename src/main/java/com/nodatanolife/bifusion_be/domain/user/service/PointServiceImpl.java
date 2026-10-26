package com.nodatanolife.bifusion_be.domain.user.service;

import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertTask;
import com.nodatanolife.bifusion_be.domain.user.config.PointProperties;
import com.nodatanolife.bifusion_be.domain.user.entity.RewardHistory;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.repository.RewardHistoryRepository;
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 호출 측 User가 트랜잭션 밖에서 로드됐을 수 있어 userId로 관리 대상 엔티티를 다시 얻어 바꾼다.
 * 금액이 0 이하면 아무것도 하지 않는다 (설정으로 끈 항목).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PointServiceImpl implements PointService {

    private final UserRepository userRepository;
    private final RewardHistoryRepository rewardHistoryRepository;
    private final PointProperties pointProperties;

    @Override
    public void earn(Long userId, int amount, String type, String targetType, Long targetId) {
        if (amount <= 0) {
            return;
        }
        User user = userRepository.getReferenceById(userId);
        user.addPoints(amount);
        rewardHistoryRepository.save(RewardHistory.of(user, amount, type, targetType, targetId));
    }

    @Override
    public void spend(Long userId, int amount, String type, String targetType, Long targetId) {
        if (amount <= 0) {
            return;
        }
        User user = userRepository.getReferenceById(userId);
        int current = user.getPoints() == null ? 0 : user.getPoints();
        if (current < amount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINTS);
        }
        user.deductPoints(amount);
        rewardHistoryRepository.save(RewardHistory.of(user, -amount, type, targetType, targetId));
    }

    /**
     * 비활성화돼 있으면 503(POINT_CHARGE_DISABLED), 상품에 없는 앤트 수면 400(POINT_CHARGE_OPTION_INVALID).
     */
    @Override
    public int charge(Long userId, int points) {
        PointProperties.Charge charge = pointProperties.charge();
        if (!charge.enabled()) {
            throw new BusinessException(ErrorCode.POINT_CHARGE_DISABLED);
        }
        if (!charge.options().contains(points)) {
            throw new BusinessException(ErrorCode.POINT_CHARGE_OPTION_INVALID);
        }
        earn(userId, points, RewardHistory.TYPE_CHARGE, null, null);
        return userRepository.getReferenceById(userId).getPoints();
    }

    @Override
    public void earnForExpertTask(Long expertId, ExpertTask task, int amount) {
        if (amount <= 0) {
            return;
        }
        User expert = userRepository.getReferenceById(expertId);
        expert.addPoints(amount);
        rewardHistoryRepository.save(RewardHistory.create(expert, task, amount, RewardHistory.TYPE_EXPERT_TASK_REWARD));
    }
}
