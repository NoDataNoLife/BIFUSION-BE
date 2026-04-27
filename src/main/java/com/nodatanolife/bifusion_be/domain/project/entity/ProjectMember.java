package com.nodatanolife.bifusion_be.domain.project.entity;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_member_id")
    private Long projectMemberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "role", length = 20)
    private String role;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "is_public")
    private Boolean isPublic;

    public static ProjectMember of(Project project, User user, String role) {
        ProjectMember member = new ProjectMember();
        member.project = project;
        member.user = user;
        member.role = role;
        member.joinedAt = LocalDateTime.now();
        // 비공개 프로젝트는 무조건 비공개로 강제, 공개 프로젝트는 기본이 공개
        member.isPublic = Boolean.TRUE.equals(project.getIsPublic());
        return member;
    }

    /**
     * 마이페이지 공개 여부 변경
     */
    public void updateVisibility(boolean isPublic) {
        this.isPublic = isPublic;
    }
}
