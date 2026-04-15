package com.nodatanolife.bifusion_be.domain.user.entity;

import com.nodatanolife.bifusion_be.domain.job.entity.Job;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usage_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UsageHistory {

    @Id
    @Column(name = "usage_history_id")
    private String usageHistoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "tier", length = 20)
    private String tier;

    @Column(name = "feature_type", length = 50)
    private String featureType;

    @Column(name = "used_tokens")
    private Long usedTokens;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
