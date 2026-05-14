package com.nodatanolife.bifusion_be.domain.job.entity;

import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;


    @Builder
    private JobComment(User user, Job job, String content) {
        this.user = user;
        this.job = job;
        this.content = content;
    }


    public static JobComment create(User user, Job job, String content) {
        return JobComment.builder()
                .user(user)
                .job(job)
                .content(content)
                .build();
    }
    public void updateUser(User newUser) {
        this.user = newUser;
    }


}