package com.nodatanolife.bifusion_be.domain.recipe.entity;

import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationParameters;
import com.nodatanolife.bifusion_be.domain.recipe.entity.converter.RecipeSettingsFormatConverter;
import com.nodatanolife.bifusion_be.global.converter.StringListConverter;
import java.util.List;
import com.nodatanolife.bifusion_be.domain.recipe.dto.request.RecipeRegisterRequest;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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
    @Column(name = "short_description", columnDefinition = "TEXT")
    private String shortDescription;

    // 상세 페이지에 노출되는 본문 설명
    @Column(name = "overview", columnDefinition = "TEXT")
    private String overview;

    // 썸네일/배너 이미지 URL
    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    // 핵심 특징 목록 (콤마 구분 문자열로 변환되어 저장됨)
    @Convert(converter = StringListConverter.class)
    @Column(name = "key_features", columnDefinition = "TEXT")
    private List<String> keyFeatures;

    // 사용 사례/추천 용도 목록 (콤마 구분 문자열로 변환되어 저장됨)
    @Convert(converter = StringListConverter.class)
    @Column(name = "use_cases", columnDefinition = "TEXT")
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

    // 아래 예전 생성 파라미터(model ~ batch_size)는 LEGACY 형식 레시피만 값이 있다
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

    // 증강 설정값 (증강 실행 파라미터와 같은 형식). AUGMENTATION 형식 레시피만 값이 있고, seed는 비어 있을 수 있다
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "augmentation_settings", columnDefinition = "jsonb")
    private AugmentationParameters augmentationSettings;

    // 설정값 형식. 이 컬럼이 생기기 전 레시피는 null이며 getSettingsFormat()에서 LEGACY로 본다
    @Convert(converter = RecipeSettingsFormatConverter.class)
    @Column(name = "settings_format", length = 20)
    private RecipeSettingsFormat settingsFormat;

    // 설정값을 불러온 증강 작업 번호 (출처 표시용). 직접 입력했거나 예전 형식이면 null
    @Column(name = "source_job_id")
    private Long sourceJobId;

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
     *
     * @param augmentationSettings 검증을 마친 설정값 (직접 입력값 또는 sourceJobId 작업에서 복사한 값)
     * @param sourceJobId          설정값을 불러온 작업 번호. 직접 입력했으면 null
     */
    public static Recipe create(RecipeRegisterRequest request, User user, Recipe forkedFrom,
                                AugmentationParameters augmentationSettings, Long sourceJobId) {
        Recipe recipe = new Recipe();
        recipe.user = user;
        recipe.forkedFrom = forkedFrom;
        recipe.title = request.title();
        recipe.shortDescription = request.description();
        recipe.overview = request.overview();
        recipe.keyFeatures = request.features();
        recipe.useCases = request.recommendations();
        recipe.applyAugmentationSettings(augmentationSettings, sourceJobId);

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
        // 예전 형식 레시피는 형식(null = LEGACY)까지 그대로 복사한다
        recipe.augmentationSettings = original.augmentationSettings;
        recipe.settingsFormat = original.settingsFormat;
        recipe.sourceJobId = original.sourceJobId;

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
     * 형식 컬럼이 생기기 전 레시피(null)는 예전 형식으로 본다.
     */
    public RecipeSettingsFormat getSettingsFormat() {
        return settingsFormat != null ? settingsFormat : RecipeSettingsFormat.LEGACY;
    }

    /**
     * 대표 이미지 지정. 값은 S3 key이며 응답 시점에 presign한다.
     */
    public void updateThumbnail(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    /**
     * 화면에 보여줄 포크 수. 포크한 레시피는 원본의 포크 수를 보여준다 (사본은 비공개라 자기 포크 수가 의미 없다).
     */
    public int getDisplayForkCount() {
        // forkedFrom은 지연 로딩 프록시일 수 있어 필드가 아니라 getter로 읽는다 (프록시의 필드는 비어 있다)
        Integer count = forkedFrom != null ? forkedFrom.getForkCount() : forkCount;
        return count != null ? count : 0;
    }

    /**
     * 이 레시피가 포크될 때마다 호출되어 forkCount를 1 증가시킨다.
     */
    public void increaseForkCount() {
        this.forkCount = this.forkCount + 1;
    }

    public void delete() {
        this.markDeleted();
    }

    /**
     * 레시피 정보 수정.
     * 프론트가 기존 값을 채운 폼 전체를 다시 제출하는 방식이라 전달받은 값 그대로 덮어쓴다.
     * 예전 형식 레시피도 수정하면 증강 설정값 형식으로 바뀐다.
     */
    public void update(String title, String shortDescription, String overview,
                       List<String> keyFeatures, List<String> useCases, Boolean isPublic,
                       AugmentationParameters augmentationSettings, Long sourceJobId) {
        this.title = title;
        this.shortDescription = shortDescription;
        this.overview = overview;
        this.keyFeatures = keyFeatures;
        this.useCases = useCases;
        if (isPublic != null) {
            this.isPublic = isPublic;
        }
        applyAugmentationSettings(augmentationSettings, sourceJobId);
    }

    /**
     * 증강 설정값 형식으로 저장한다. 예전 생성 파라미터는 새 설정값과 섞이지 않도록 비운다.
     */
    private void applyAugmentationSettings(AugmentationParameters augmentationSettings, Long sourceJobId) {
        this.augmentationSettings = augmentationSettings;
        this.settingsFormat = RecipeSettingsFormat.AUGMENTATION;
        this.sourceJobId = sourceJobId;
        this.model = null;
        this.steps = null;
        this.sampler = null;
        this.cfgScale = null;
        this.seed = null;
        this.resolution = null;
        this.batchSize = null;
    }

}
