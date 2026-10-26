package com.nodatanolife.bifusion_be.user;

import com.nodatanolife.bifusion_be.domain.expert.service.ExpertService;
import com.nodatanolife.bifusion_be.domain.qna.dto.request.ExpertQnAAnswerRequest;
import com.nodatanolife.bifusion_be.domain.qna.dto.request.ExpertQnaRequest;
import com.nodatanolife.bifusion_be.domain.qna.service.ExpertQnAService;
import com.nodatanolife.bifusion_be.domain.recipe.service.RecipeForkService;
import com.nodatanolife.bifusion_be.domain.user.entity.RewardHistory;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.service.PointService;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2UserService;
import com.nodatanolife.bifusion_be.support.IntegrationTestSupport;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 가입 보너스, 레시피 포크, Q&A 등록·답변, 앤트 충전, 전문가 성과 총 리워드.
 */
class PointIntegrationTest extends IntegrationTestSupport {

    @Autowired
    CustomOAuth2UserService customOAuth2UserService;
    @Autowired
    RecipeForkService recipeForkService;
    @Autowired
    ExpertQnAService expertQnAService;
    @Autowired
    ExpertService expertService;
    @Autowired
    PointService pointService;

    @Test
    void 가입하면_500앤트를_받고_내역이_남는다() {
        User newbie = ReflectionTestUtils.invokeMethod(customOAuth2UserService, "createUserWithPendingInvitations",
                "newbie@test.com", "newbie");

        assertThat(pointsOf(newbie)).isEqualTo(500);
        assertThat(rewardHistoryAmount(newbie, RewardHistory.TYPE_SIGNUP_BONUS)).isEqualTo(500);
    }

    // ---------- 포크 ----------

    @Test
    void 다른_사람_레시피를_포크하면_30앤트가_차감되고_원작자는_27앤트를_받는다() {
        User author = user("author@test.com", 0);
        User forker = user("forker@test.com", 100);
        Long recipeId = recipe(author, null, true);

        recipeForkService.forkRecipe(recipeId, forker.getUserId());

        assertThat(pointsOf(forker)).isEqualTo(70);
        assertThat(pointsOf(author)).isEqualTo(27);
        assertThat(rewardHistoryAmount(forker, RewardHistory.TYPE_RECIPE_FORK)).isEqualTo(-30);
        assertThat(rewardHistoryAmount(author, RewardHistory.TYPE_RECIPE_FORK_REWARD)).isEqualTo(27);
    }

    @Test
    void 내_레시피를_포크하면_주고받지_않는다() {
        User author = user("author@test.com", 0);
        Long recipeId = recipe(author, null, true);

        recipeForkService.forkRecipe(recipeId, author.getUserId());

        assertThat(pointsOf(author)).isZero();
        assertThat(rewardHistoryCount(author, RewardHistory.TYPE_RECIPE_FORK)).isZero();
    }

    @Test
    void 포인트가_부족하면_포크가_막히고_사본도_남지_않는다() {
        User author = user("author@test.com", 0);
        User forker = user("forker@test.com", 10);
        Long recipeId = recipe(author, null, true);
        long before = recipeRepository.count();

        assertError(() -> recipeForkService.forkRecipe(recipeId, forker.getUserId()), ErrorCode.INSUFFICIENT_POINTS);

        assertThat(recipeRepository.count()).isEqualTo(before);
        assertThat(pointsOf(forker)).isEqualTo(10);
        assertThat(pointsOf(author)).isZero();
    }

    // ---------- Q&A ----------

    @Test
    void 질문을_등록하면_100앤트가_차감된다() {
        User asker = user("asker@test.com", 150);

        expertQnAService.createQna(asker.getUserId(), question());

        assertThat(pointsOf(asker)).isEqualTo(50);
        assertThat(rewardHistoryAmount(asker, RewardHistory.TYPE_QNA_QUESTION)).isEqualTo(-100);
    }

    @Test
    void 포인트가_부족하면_질문이_막히고_질문도_남지_않는다() {
        User asker = user("asker@test.com", 50);

        assertError(() -> expertQnAService.createQna(asker.getUserId(), question()), ErrorCode.INSUFFICIENT_POINTS);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM expert_qna", Integer.class)).isZero();
        assertThat(pointsOf(asker)).isEqualTo(50);
    }

    @Test
    void 전문가가_답변하면_90앤트를_받고_같은_질문에는_한_번만_받는다() {
        User asker = user("asker@test.com", 100);
        User expert = expert("expert@test.com", 0);
        Long qnaId = expertQnAService.createQna(asker.getUserId(), question()).qnaId();

        expertQnAService.createAnswer(expert.getUserId(), qnaId, answer());
        expertQnAService.createAnswer(expert.getUserId(), qnaId, answer());

        assertThat(pointsOf(expert)).isEqualTo(90);
        assertThat(rewardHistoryCount(expert, RewardHistory.TYPE_QNA_ANSWER_REWARD)).isEqualTo(1);
    }

    @Test
    void 여러_전문가가_답변하면_각자_받는다() {
        User asker = user("asker@test.com", 100);
        User first = expert("first@test.com", 0);
        User second = expert("second@test.com", 0);
        Long qnaId = expertQnAService.createQna(asker.getUserId(), question()).qnaId();

        expertQnAService.createAnswer(first.getUserId(), qnaId, answer());
        expertQnAService.createAnswer(second.getUserId(), qnaId, answer());

        assertThat(pointsOf(first)).isEqualTo(90);
        assertThat(pointsOf(second)).isEqualTo(90);
    }

    @Test
    void 본인_질문에_단_답변은_보상하지_않는다() {
        User expert = expert("expert@test.com", 100);
        Long qnaId = expertQnAService.createQna(expert.getUserId(), question()).qnaId();

        expertQnAService.createAnswer(expert.getUserId(), qnaId, answer());

        assertThat(pointsOf(expert)).isZero();
        assertThat(rewardHistoryCount(expert, RewardHistory.TYPE_QNA_ANSWER_REWARD)).isZero();
    }

    // ---------- 충전 ----------

    @Test
    void 충전_상품만큼_앤트가_늘고_내역이_남는다() {
        User user = user("user@test.com", 100);

        int balance = pointService.charge(user.getUserId(), 500);

        assertThat(balance).isEqualTo(600);
        assertThat(pointsOf(user)).isEqualTo(600);
        assertThat(rewardHistoryAmount(user, RewardHistory.TYPE_CHARGE)).isEqualTo(500);
    }

    @Test
    void 충전_상품에_없는_앤트_수는_충전할_수_없다() {
        User user = user("user@test.com", 100);

        assertError(() -> pointService.charge(user.getUserId(), 700), ErrorCode.POINT_CHARGE_OPTION_INVALID);
        assertThat(pointsOf(user)).isEqualTo(100);
    }

    // ---------- 전문가 성과 ----------

    @Test
    void 전문가_성과의_총_리워드는_전문가_활동_보상만_더한다() {
        User asker = user("asker@test.com", 100);
        User expert = expert("expert@test.com", 0);
        Long qnaId = expertQnAService.createQna(asker.getUserId(), question()).qnaId();
        expertQnAService.createAnswer(expert.getUserId(), qnaId, answer());
        pointService.charge(expert.getUserId(), 1000);

        assertThat(expertService.getMyPerformance(reload(expert)).totalReward()).isEqualTo(90);
    }

    private ExpertQnaRequest question() {
        return request(ExpertQnaRequest.class, Map.of("title", "질문", "content", "내용", "tags", List.of()));
    }

    private ExpertQnAAnswerRequest answer() {
        return request(ExpertQnAAnswerRequest.class, Map.of("content", "답변"));
    }

    private static void assertError(ThrowableAssert.ThrowingCallable call, ErrorCode errorCode) {
        assertThatThrownBy(call).isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(errorCode);
    }
}
