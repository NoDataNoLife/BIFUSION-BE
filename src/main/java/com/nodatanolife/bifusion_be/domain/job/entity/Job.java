package com.nodatanolife.bifusion_be.domain.job.entity;

import com.nodatanolife.bifusion_be.domain.project.entity.Project;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "job")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Job extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "job_code", length = 20)
    private String jobCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "type", length = 20)
    private String type;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "progress")
    private Integer progress;

    @Column(name = "seed_number", length = 20)
    private String seedNumber;

    public void updateUser(User newUser) {
        this.user = newUser;
    }

    public void anonymize(User deletedUser) {
        this.user = deletedUser;
    }
}
