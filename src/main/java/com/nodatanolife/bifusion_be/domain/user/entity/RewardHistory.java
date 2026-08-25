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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reward_history_id")
    private Long rewardHistoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expert_task_id", nullable = false)
    private ExpertTask expertTask;

    @Column(name = "amount")
    private Integer amount;

    @Column(name = "type", length = 20)
    private String type;

    @Builder
    private RewardHistory(User user, ExpertTask expertTask, Integer amount, String type) {
        this.user = user;
        this.expertTask = expertTask;
        this.amount = amount;
        this.type = type;
    }

    public static RewardHistory create(User user, ExpertTask expertTask, Integer amount, String type) {
        return RewardHistory.builder()
                .user(user)
                .expertTask(expertTask)
                .amount(amount)
                .type(type)
                .build();
    }
}
