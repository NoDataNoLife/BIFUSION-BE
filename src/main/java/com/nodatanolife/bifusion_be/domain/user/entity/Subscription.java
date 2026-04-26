package com.nodatanolife.bifusion_be.domain.user.entity;

import com.nodatanolife.bifusion_be.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id")
    private Long subscriptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "tier", length = 20)
    private String tier;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "amount")
    private Integer amount;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Builder
    public Subscription(User user, String tier, LocalDateTime startedAt, LocalDateTime expiresAt, Integer amount, String paymentMethod) {
        this.user = user;
        this.tier = tier;
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
    }
}
