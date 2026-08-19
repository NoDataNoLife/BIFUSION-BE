package com.nodatanolife.bifusion_be.domain.recipe.entity;

import com.nodatanolife.bifusion_be.global.converter.StringListConverter;
import java.util.List;
import com.nodatanolife.bifusion_be.domain.recipe.dto.request.RecipeRegisterRequest;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * 레시피 엔티티.
 * <p>
 * Dataset과 마찬가지로 "커뮤니티(전체 공개)"와 "내 자산(비공개)"이 별도 테이블이 아니라
 * 이 엔티티 하나를 {@link #isPublic} 값으로 구분해서 공유한다.
 * - isPublic = true  : 커뮤니티 목록/상세에도 노출되는 공개 레시피
 * - isPublic = false : 작성자 본인의 "내 자산" 목록/상세에서만 조회 가능한 비공개 레시피
 * <p>
 * 포크({@link #fork})로 생성된 사본은 항상 isPublic = false로 시작한다 — 원본을 복제해
 * "내 보관함"에 저장하는 개인용 사본이므로 커뮤니티에 자동 공개되지 않는다.
 */
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

    // 작성자(소유자). 공개 여부와 무관하게 항상 존재해야 하므로 nullable = false
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 레시피 제목
    @Column(name = "title")
    private String title;

    // 목록에 노출되는 짧은 설명
    @Column(name = "short_description")
    private String shortDescription;

    // 상세 페이지에 노출되는 본문 설명
    @Column(name = "overview")
    private String overview;

    // 썸네일/배너 이미지 URL
    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    // 핵심 특징 목록 (콤마 구분 문자열로 변환되어 저장됨)
    @Convert(converter = StringListConverter.class)
    @Column(name = "key_features")
    private List<String> keyFeatures;

    // 사용 사례/추천 용도 목록 (콤마 구분 문자열로 변환되어 저장됨)
    @Convert(converter = StringListConverter.class)
    @Column(name = "use_cases")
    private List<String> useCases;

    // 전문가 검증 완료 여부
    @Column(name = "is_expert_verified")
    private Boolean isExpertVerified;

    // 평균 평점 (리뷰 별점 기반)
    @Column(name = "rating")
    private BigDecimal rating;

    // 좋아요 수
    @Column(name = "like_count")
    private Integer likeCount;

    // 포크(복제)된 횟수. 원본 레시피가 포크될 때마다 증가한다
    @Column(name = "fork_count")
    private Integer forkCount;

    // 조회수
    @Column(name = "view_count")
    private Integer viewCount;

    // 다운로드 횟수
    @Column(name = "download_count")
    private Integer downloadCount;

    // 공개 여부. true면 커뮤니티 목록/상세에 노출, false면 작성자 본인만 조회 가능
    @Column(name = "is_public")
    private Boolean isPublic;

    // 마이페이지(활동 내역)에서의 노출 여부. isPublic과는 별개의 축(개인정보 노출 제어용)
    @Column(name = "is_mypage_visible")
    private Boolean isMyPageVisible;

    // 생성에 사용된 모델명
    @Column(name = "model", length = 100)
    private String model;

    // 생성 스텝 수
    @Column(name = "steps")
    private Integer steps;

    // 샘플러 종류
    @Column(name = "sampler", length = 50)
    private String sampler;

    // CFG Scale 값
    @Column(name = "cfg_scale")
    private BigDecimal cfgScale;

    // 시드값 (재현을 위해 문자열로 저장)
    @Column(name = "seed", length = 20)
    private String seed;

    // 출력 해상도
    @Column(name = "resolution", length = 20)
    private String resolution;

    // 배치 사이즈
    @Column(name = "batch_size")
    private Integer batchSize;

    /**
     * 회원 탈퇴 시 작성자를 탈퇴 계정으로 치환한다 (데이터는 남기되 개인 식별은 제거).
     */
    public void anonymize(User deletedUser) {
        this.user = deletedUser;
    }

    /**
     * 마이페이지 활동 공개 여부 변경
     */
    public void updateMyPageVisibility(boolean visible) {
        this.isMyPageVisible = visible;
    }

    /**
     * 레시피 생성 팩토리 메서드. 일반 등록과 포크 등록을 함께 처리한다
     * (forkedFrom이 null이 아니면 포크 등록).
     * request.isPublic()이 null이면 기존과 동일하게 공개(true)로 기본 처리한다.
     */
    public static Recipe create(RecipeRegisterRequest request, User user, Recipe forkedFrom) {
        Recipe recipe = new Recipe();
        recipe.user = user;
        recipe.forkedFrom = forkedFrom;
        recipe.title = request.title();
        recipe.shortDescription = request.description();
        recipe.overview = request.overview();
        recipe.keyFeatures = request.features();
        recipe.useCases = request.recommendations();
        recipe.model = request.modelName();
        recipe.steps = request.steps();
        recipe.sampler = request.sampler();
        recipe.cfgScale = BigDecimal.valueOf(request.cfgScale());
        recipe.seed = String.valueOf(request.seed());
        recipe.resolution = request.resolution();
        recipe.batchSize = request.batchSize();

        // 요청에 isPublic이 없으면(null) 기존 커뮤니티 API와 동일하게 공개(true)로 기본 처리한다.
        recipe.isPublic = request.isPublic() != null ? request.isPublic() : true;
        recipe.isExpertVerified = false;
        recipe.rating = BigDecimal.ZERO;
        recipe.likeCount = 0;
        recipe.forkCount = 0;
        recipe.viewCount = 0;
        recipe.downloadCount = 0;
        recipe.isMyPageVisible = true;

        return recipe;
    }

    /**
     * 원본 레시피(original)를 복제해 요청자(user) 소유의 새 레시피를 만든다.
     * 포크 사본은 항상 isPublic = false로 시작해 개인 보관함 용도로만 사용된다.
     */
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

    /**
     * 이 레시피가 포크될 때마다 호출되어 forkCount를 1 증가시킨다.
     */
    public void increaseForkCount() {
        this.forkCount = this.forkCount + 1;
    }

}
