
package com.nodatanolife.bifusion_be.domain.dataset.entity;

import com.nodatanolife.bifusion_be.domain.dataset.dto.request.DatasetCreateRequest;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.entity.BaseEntity;
import com.nodatanolife.bifusion_be.domain.file.entity.File;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 데이터셋 엔티티.
 * <p>
 * "커뮤니티(전체 공개)"와 "내 자산(비공개)"은 서로 다른 테이블/도메인이 아니라
 * 이 엔티티 하나를 {@link #isPublic} 값으로 구분해서 공유한다.
 * - isPublic = true  : 커뮤니티 목록/상세에도 노출되는 공개 데이터셋
 * - isPublic = false : 작성자 본인의 "내 자산" 목록/상세에서만 조회 가능한 비공개 데이터셋
 */
@Entity
@Table(name = "dataset")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dataset extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dataset_id")
    private Long datasetId;

    // 작성자(소유자). 공개 여부와 무관하게 항상 존재해야 하므로 nullable = false
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 데이터셋 제목
    @Column(name = "title")
    private String title;

    // 데이터셋 설명
    @Column(name = "description")
    private String description;

    // 분류 카테고리 (예: 이미지, 텍스트 등)
    @Column(name = "category", length = 50)
    private String category;

    // 데이터셋 고유 코드 (표시용 짧은 식별자)
    @Column(name = "dataset_code", length = 20)
    private String datasetCode;

    // 라이선스 정보
    @Column(name = "license", length = 50)
    private String license;

    // 파일 포맷 (예: CSV, JSON, JPG 등)
    @Column(name = "format", length = 20)
    private String format;

    // 이미지 데이터셋인 경우의 이미지 타입
    @Column(name = "image_type", length = 50)
    private String imageType;

    // 이미지 해상도 정보
    @Column(name = "resolution", length = 50)
    private String resolution;

    // 데이터셋에 포함된 클래스(라벨) 목록 설명
    @Column(name = "classes")
    private String classes;

    // 사용 예시 설명
    @Column(name = "usage_example")
    private String usageExample;

    // 첨부 파일의 URL 캐시 (file 연관관계의 fileUrl을 그대로 복사해 둔 값)
    @Column(name = "file_url")
    private String fileUrl;

    // 공개 여부. true면 커뮤니티 목록/상세에 노출, false면 작성자 본인만 조회 가능
    @Column(name = "is_public")
    private Boolean isPublic;

    // 마이페이지(활동 내역)에서의 노출 여부. isPublic과는 별개의 축(개인정보 노출 제어용)
    @Column(name = "is_mypage_visible")
    private Boolean isMyPageVisible;

    // 전문가 검증 완료 여부
    @Column(name = "is_expert_verified")
    private Boolean isExpertVerified;

    // 데이터셋에 포함된 파일 개수
    @Column(name = "file_count")
    private Integer fileCount;

    // 다운로드 횟수
    @Column(name = "download_count")
    private Integer downloadCount;

    // 좋아요 수
    @Column(name = "like_count")
    private Integer likeCount;

    // 조회수
    @Column(name = "view_count")
    private Integer viewCount;

    // 인용(citation) 정보
    @Column(name = "citation")
    private String citation;

    // 데이터셋 종류: 업로드 데이터 / 증강 데이터 (내 자산 목록 조회 시 필터 기준)
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20)
    private DatasetType type;

    // 목록/상세에서 보여줄 배너 이미지 URL
    @Column(name = "banner_image_url")
    private String bannerImageUrl;

    // 실제 업로드된 파일과의 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private File file;

    // soft delete 시각. null이면 삭제되지 않은 상태
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;


    @Builder
    private Dataset(User user, String title, String description, String category,
                    String license, String format, String imageType, String resolution,
                    String classes, String usageExample, File file, Boolean isPublic) {
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
        this.file = file;
        this.fileUrl = file.getFileUrl();
        // 요청에 isPublic이 없으면(null) 기존 커뮤니티 API와 동일하게 공개(true)로 기본 처리한다.
        this.isPublic = isPublic != null ? isPublic : true;
        this.isMyPageVisible = true;
        this.isExpertVerified = false;
        this.fileCount = 0;
        this.downloadCount = 0;
        this.likeCount = 0;
        this.viewCount = 0;
        this.type = DatasetType.UPLOADED;
    }

    /**
     * 데이터셋 생성 팩토리 메서드.
     * request.getIsPublic()이 null이면 생성자에서 공개(true)로 기본 처리된다.
     */
    public static Dataset create(User user, DatasetCreateRequest request,  File file) {
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
                .file(file)
                .isPublic(request.getIsPublic())
                .build();
    }

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
     * soft delete 처리. deletedAt에 현재 시각을 기록한다 (BaseEntity#markDeleted).
     */
    public void delete() {
        markDeleted();
    }

    /**
     * 데이터셋 정보 수정. 프론트가 기존 값을 불러와 채운 폼 전체를 다시 제출하는 방식이라
     * 부분 수정(null 무시)이 아닌 전달받은 값 그대로 덮어쓴다 (title/description은 필수라 항상 값이 있음).
     */
    public void update(String title, String description, String category, String license,
                        String format, String imageType, String resolution, String classes,
                        String usageExample) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.license = license;
        this.format = format;
        this.imageType = imageType;
        this.resolution = resolution;
        this.classes = classes;
        this.usageExample = usageExample;
    }

    /**
     * 첨부 파일 교체. 수정 요청에 새 fileId가 포함된 경우에만 호출되며,
     * 생략 시(기존 파일 유지) 이 메서드는 호출되지 않는다.
     */
    public void updateFile(File file) {
        this.file = file;
        this.fileUrl = file.getFileUrl();
    }
}
