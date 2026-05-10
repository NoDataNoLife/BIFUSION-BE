package com.nodatanolife.bifusion_be.domain.dataset.entity;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dataset")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dataset extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dataset_id")
    private Long datasetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "dataset_code", length = 20)
    private String datasetCode;

    @Column(name = "license", length = 50)
    private String license;

    @Column(name = "format", length = 20)
    private String format;

    @Column(name = "image_type", length = 50)
    private String imageType;

    @Column(name = "resolution", length = 50)
    private String resolution;

    @Column(name = "classes")
    private String classes;

    @Column(name = "usage_example")
    private String usageExample;

    @Column(name = "is_public")
    private Boolean isPublic;

    @Column(name = "is_mypage_visible")
    private Boolean isMyPageVisible;

    @Column(name = "is_expert_verified")
    private Boolean isExpertVerified;

    @Column(name = "file_count")
    private Integer fileCount;

    @Column(name = "download_count")
    private Integer downloadCount;

    @Column(name = "like_count")
    private Integer likeCount;

    @Column(name = "view_count")
    private Integer viewCount;

    @Column(name = "citation")
    private String citation;

    public void anonymize(User deletedUser) {
        this.user = deletedUser;
    }

    /**
     * 마이페이지 활동 공개 여부 변경
     */
    public void updateMyPageVisibility(boolean visible) {
        this.isMyPageVisible = visible;
    }
}
