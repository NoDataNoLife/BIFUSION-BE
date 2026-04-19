package com.nodatanolife.bifusion_be.domain.project.entity;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "project_favorite")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectFavorite extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_favorite_id")
    private Long projectFavoriteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
}
