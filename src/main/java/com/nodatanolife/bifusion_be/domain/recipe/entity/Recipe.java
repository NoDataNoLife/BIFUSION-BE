package com.nodatanolife.bifusion_be.domain.recipe.entity;

import com.nodatanolife.bifusion_be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recipe")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipe_id")
    private Long recipeId;

    // 자기 참조 (포크 원본). null 허용을 위해 nullable = true
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forked_from_id", nullable = true)
    private Recipe forkedFrom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title")
    private String title;

    @Column(name = "short_description")
    private String shortDescription;

    @Column(name = "overview")
    private String overview;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "key_features")
    private String keyFeatures;

    @Column(name = "use_cases")
    private String useCases;

    @Column(name = "is_expert_verified")
    private Boolean isExpertVerified;

    @Column(name = "rating")
    private BigDecimal rating;

    @Column(name = "like_count")
    private Integer likeCount;

    @Column(name = "fork_count")
    private Integer forkCount;

    @Column(name = "view_count")
    private Integer viewCount;

    @Column(name = "download_count")
    private Integer downloadCount;

    @Column(name = "is_public")
    private Boolean isPublic;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "steps")
    private Integer steps;

    @Column(name = "sampler", length = 50)
    private String sampler;

    @Column(name = "cfg_scale")
    private BigDecimal cfgScale;

    @Column(name = "seed", length = 20)
    private String seed;

    @Column(name = "resolution", length = 20)
    private String resolution;

    @Column(name = "batch_size")
    private Integer batchSize;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
