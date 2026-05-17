
package com.nodatanolife.bifusion_be.domain.dataset.entity;

import com.nodatanolife.bifusion_be.domain.dataset.dto.request.DatasetCreateRequest;
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

    @Column(name = "file_url")
    private String fileUrl;

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

    @Builder
    private Dataset(User user, String title, String description, String category,
                    String license, String format, String imageType, String resolution,
                    String classes, String usageExample, String fileUrl) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.category = category;
        this.license = license;
        this.format = format;
        this.imageType = imageType;
        this.resolution = resolution;
        this.classes = classes;
        this.usageExample = usageExample;
        this.fileUrl = fileUrl;
        this.isPublic = true;
        this.isMyPageVisible = true;
        this.isExpertVerified = false;
        this.fileCount = 0;
        this.downloadCount = 0;
        this.likeCount = 0;
        this.viewCount = 0;
    }

    public static Dataset create(User user, DatasetCreateRequest request, String fileUrl) {
        return Dataset.builder()
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .license(request.getLicense())
                .format(request.getFormat())
                .imageType(request.getImageType())
                .resolution(request.getResolution())
                .classes(request.getClasses())
                .usageExample(request.getUsageExample())
                .fileUrl(fileUrl)
                .build();
    }

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



