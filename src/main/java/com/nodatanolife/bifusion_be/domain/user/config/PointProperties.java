package com.nodatanolife.bifusion_be.domain.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 서비스 이용에 따른 포인트(앤트) 지급·차감 금액 (points). 임시 단가 1앤트 = 10원.
 * 전문가 검수 리워드 범위는 inspection.reward에서 따로 정한다.
 *
 * @param signupBonus        가입 시 지급
 * @param recipeForkCost     다른 사람 레시피를 포크할 때 차감
 * @param recipeForkReward   포크된 원본 작성자에게 지급 (차감액에서 수수료 10%를 뗀 값)
 * @param qnaQuestionCost    전문가 Q&A 질문 등록 시 차감
 * @param qnaAnswerReward    전문가 답변 시 지급 (질문 하나당 전문가 한 명에게 1회)
 * @param charge             앤트 충전 (결제 연동 전 체험용)
 */
@ConfigurationProperties(prefix = "points")
public record PointProperties(
        int signupBonus,
        int recipeForkCost,
        int recipeForkReward,
        int qnaQuestionCost,
        int qnaAnswerReward,
        Charge charge
) {

    /**
     * @param enabled 충전 API 사용 여부. 결제 연동 전에는 요청만으로 앤트가 지급되므로 운영에서 막으려면 false
     * @param options 충전할 수 있는 앤트 수 (충전 화면의 상품과 같아야 한다)
     */
    public record Charge(
            boolean enabled,
            List<Integer> options
    ) {
    }
}
