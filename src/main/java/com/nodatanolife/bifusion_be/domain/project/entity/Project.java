package com.nodatanolife.bifusion_be.domain.project.entity;

import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "project")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "banner_image_url")
    private String bannerImageUrl;

    @Column(name = "is_public")
    private Boolean isPublic;

    private Project(String title, String description, Boolean isPublic) {
        this.title = title;
        this.description = description;
        this.isPublic = (isPublic != null) ? isPublic : true;
    }

    public static Project of(String title, String description, Boolean isPublic) {
        return new Project(title, description, isPublic);
    }

    public void update(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public void updateBannerImage(String bannerImageUrl) {
        this.bannerImageUrl = bannerImageUrl;
    }

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectMember> projectMembers = new ArrayList<>();
}
