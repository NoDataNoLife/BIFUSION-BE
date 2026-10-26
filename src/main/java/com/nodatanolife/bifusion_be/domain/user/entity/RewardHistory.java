package com.nodatanolife.bifusion_be.domain.user.entity;

import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertTask;
import com.nodatanolife.bifusion_be.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reward_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RewardHistory extends BaseCreatedEntity {

    public static final String TYPE_EXPERT_TASK_REWARD = "EXPERT_TASK_REWARD";
    public static final String TYPE_QNA_ANSWER_REWARD = "QNA_ANSWER_REWARD";
    public static final String TYPE_SIGNUP_BONUS = "SIGNUP_BONUS";
    public static final String TYPE_RECIPE_FORK = "RECIPE_FORK";
    public static final String TYPE_RECIPE_FORK_REWARD = "RECIPE_FORK_REWARD";
    public static final String TYPE_QNA_QUESTION = "QNA_QUESTION";
    public static final String TYPE_INSPECTION_REQUEST = "INSPECTION_REQUEST";
    public static final String TYPE_INSPECTION_REFUND = "INSPECTION_REFUND";

    public static final String TARGET_RECIPE = "RECIPE";
    public static final String TARGET_QNA = "QNA";
    public static final String TARGET_INSPECTION_REQUEST = "INSPECTION_REQUEST";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reward_history_id")
    private Long rewardHistoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 전문가 검수 보상일 때만 채운다. 그 외 내역은 targetType·targetId로 대상을 가리킨다.
    // 기존 DB의 NOT NULL 제약은 직접 풀어야 한다 (ALTER TABLE reward_history ALTER COLUMN expert_task_id DROP NOT NULL)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expert_task_id")
    private ExpertTask expertTask;

    // 양수는 지급, 음수는 차감
    @Column(name = "amount")
    private Integer amount;

    // TYPE_* 상수. 컬럼 길이가 20이라 그 이하로 짓는다
    @Column(name = "type", length = 20)
    private String type;

    // 내역이 가리키는 대상 종류 (TARGET_*). 대상이 없으면(가입 보너스) null
    @Column(name = "target_type", length = 30)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Builder
    private RewardHistory(User user, ExpertTask expertTask, Integer amount, String type, String targetType, Long targetId) {
        this.user = user;
        this.expertTask = expertTask;
        this.amount = amount;
        this.type = type;
        this.targetType = targetType;
        this.targetId = targetId;
    }

    public static RewardHistory create(User user, ExpertTask expertTask, Integer amount, String type) {
        return RewardHistory.builder()
                .user(user)
                .expertTask(expertTask)
                .amount(amount)
                .type(type)
                .build();
    }

    /**
     * 검수 보상 외의 지급·차감 내역.
     *
     * @param amount 지급은 양수, 차감은 음수
     */
    public static RewardHistory of(User user, Integer amount, String type, String targetType, Long targetId) {
        return RewardHistory.builder()
                .user(user)
                .amount(amount)
                .type(type)
                .targetType(targetType)
                .targetId(targetId)
                .build();
    }
}
