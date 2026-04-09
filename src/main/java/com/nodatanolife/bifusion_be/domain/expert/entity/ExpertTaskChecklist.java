package com.nodatanolife.bifusion_be.domain.expert.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "expert_task_checklist")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpertTaskChecklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expert_task_checklist_id")
    private Long expertTaskChecklistId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expert_task_id", nullable = false)
    private ExpertTask expertTask;

    @Column(name = "item_number")
    private Short itemNumber;

    @Column(name = "is_checked")
    private Boolean isChecked;
}
