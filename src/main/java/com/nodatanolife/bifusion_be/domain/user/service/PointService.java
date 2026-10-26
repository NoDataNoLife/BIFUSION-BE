package com.nodatanolife.bifusion_be.domain.user.service;

import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertTask;

/**
 * 포인트(앤트) 지급·차감. 잔액(User.points) 변경과 내역(RewardHistory) 기록을 항상 함께 한다.
 * 포인트를 바꾸는 코드는 User.addPoints/deductPoints를 직접 부르지 않고 이 서비스를 쓴다.
 */
public interface PointService {

    /**
     * @param targetType RewardHistory.TARGET_* (대상이 없으면 null)
     */
    void earn(Long userId, int amount, String type, String targetType, Long targetId);

    /**
     * 잔액이 부족하면 400(INSUFFICIENT_POINTS)이며 아무것도 바꾸지 않는다.
     */
    void spend(Long userId, int amount, String type, String targetType, Long targetId);

    /**
     * 전문가 검수 보상. 내역에 ExpertTask를 연결한다.
     */
    void earnForExpertTask(Long expertId, ExpertTask task, int amount);

    /**
     * 앤트 충전 (결제 연동 전 체험용, 결제 없이 지급). 설정한 상품만 충전할 수 있다.
     *
     * @return 충전 후 잔액
     */
    int charge(Long userId, int points);
}
