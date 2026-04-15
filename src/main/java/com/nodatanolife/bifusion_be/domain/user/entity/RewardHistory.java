package com.nodatanolife.bifusion_be.domain.user.entity;

import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertTask;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reward_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RewardHistory {

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

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
