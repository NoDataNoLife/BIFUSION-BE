package com.nodatanolife.bifusion_be.domain.recipe.entity;

import com.nodatanolife.bifusion_be.domain.recipe.dto.request.RecipeUpdateRequest;
import com.nodatanolife.bifusion_be.global.converter.StringListConverter;
import java.util.List;
import com.nodatanolife.bifusion_be.domain.recipe.dto.request.RecipeRegisterRequest;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "recipe")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recipe extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipe_id")
    private Long recipeId;

    // 자기 참조 (포크 원본). null 허용을 위해 nullable = true
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forked_from_id")
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

    @Convert(converter = StringListConverter.class)
    @Column(name = "key_features")
    private List<String> keyFeatures;

    @Convert(converter = StringListConverter.class)
    @Column(name = "use_cases")
    private List<String> useCases;

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

    @Column(name = "is_mypage_visible")
    private Boolean isMyPageVisible;

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

    public void anonymize(User deletedUser) {
        this.user = deletedUser;
    }

    /**
     * 마이페이지 활동 공개 여부 변경
     */
    public void updateMyPageVisibility(boolean visible) {
        this.isMyPageVisible = visible;
    }

    public static Recipe create(RecipeRegisterRequest request, User user) {
        Recipe recipe = new Recipe();
        recipe.user = user;
        recipe.forkedFrom = null;
        recipe.title = request.title();
        recipe.shortDescription = request.description();
        recipe.model = request.modelName();
        recipe.steps = request.steps();
        recipe.sampler = request.sampler();
        recipe.cfgScale = BigDecimal.valueOf(request.cfgScale());
        recipe.seed = String.valueOf(request.seed());
        recipe.resolution = request.resolution();
        recipe.batchSize = request.batchSize();

        recipe.isPublic = true;
        recipe.isExpertVerified = false;
        recipe.rating = BigDecimal.ZERO;
        recipe.likeCount = 0;
        recipe.forkCount = 0;
        recipe.viewCount = 0;
        recipe.downloadCount = 0;
        recipe.isMyPageVisible = true;

        return recipe;
    }

    public static Recipe fork(Recipe original, User user) {
        Recipe recipe = new Recipe();
        recipe.user = user;
        recipe.forkedFrom = original;
        recipe.title = original.title;
        recipe.shortDescription = original.shortDescription;
        recipe.overview = original.overview;
        recipe.thumbnailUrl = original.thumbnailUrl;
        recipe.keyFeatures = original.keyFeatures;
        recipe.useCases = original.useCases;
        recipe.model = original.model;
        recipe.steps = original.steps;
        recipe.sampler = original.sampler;
        recipe.cfgScale = original.cfgScale;
        recipe.seed = original.seed;
        recipe.resolution = original.resolution;
        recipe.batchSize = original.batchSize;

        recipe.isPublic = false;
        recipe.isExpertVerified = false;
        recipe.rating = BigDecimal.ZERO;
        recipe.likeCount = 0;
        recipe.forkCount = 0;
        recipe.viewCount = 0;
        recipe.downloadCount = 0;
        recipe.isMyPageVisible = true;

        return recipe;
    }

    public void increaseForkCount() {
        this.forkCount = this.forkCount + 1;
    }

    public void update(RecipeUpdateRequest request) {
        this.title = request.title();
        this.shortDescription = request.description();
        this.model = request.modelName();
        this.steps = request.steps();
        this.sampler = request.sampler();
        this.cfgScale = BigDecimal.valueOf(request.cfgScale());
        this.seed = String.valueOf(request.seed());
        this.resolution = request.resolution();
        this.batchSize = request.batchSize();
    }

}
