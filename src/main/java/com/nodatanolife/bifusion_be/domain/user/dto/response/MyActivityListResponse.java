package com.nodatanolife.bifusion_be.domain.user.dto.response;

import com.nodatanolife.bifusion_be.domain.dataset.entity.Dataset;
import com.nodatanolife.bifusion_be.domain.file.presign.PresignedUrl;
import com.nodatanolife.bifusion_be.domain.qna.entity.ExpertQnA;
import com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe;
import com.nodatanolife.bifusion_be.domain.recruitment.entity.Recruit;
import com.nodatanolife.bifusion_be.domain.user.entity.ActivityType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 마이페이지 "내 활동" 커서 기반 목록 조회 응답.
 * Recipe/Dataset/ExpertQnA/Recruit를 최신순으로 병합한 결과이며,
 * 본인 조회이므로 isPublic/isMyPageVisible과 무관하게 소유한 항목을 전부 담는다.
 * isPublic은 글 자체의 공개 여부, isMyPageVisible은 공개 프로필에 표시되는지(비공개 글이면 항상 false)다.
 * <p>
 * 좋아요/댓글/다운로드 수처럼 도메인마다 있고 없고가 갈리는 값은 null로 채우는 대신,
 * 타입별 record({@link RecipeItem}/{@link DatasetItem}/{@link QnaItem}/{@link RecruitItem})로
 * 나눠 애초에 해당 도메인에 없는 필드는 구조적으로 존재하지 않게 한다.
 * 타입별로 실제 내려가는 필드:
 * <pre>
 * RECIPE      : thumbnailUrl, likeCount, commentCount (최상위 리뷰 + 대댓글, 레시피 상세 reviewCount와 같은 기준), forkCount
 * DATASET     : thumbnailUrl, likeCount, downloadCount
 * QNA         : commentCount (ExpertQnA.answerCount = 전문가 답변 수)
 * RECRUITMENT : commentCount (RecruitApplication 총 지원 수, 상태 무관)
 * </pre>
 */
public record MyActivityListResponse(
        List<Item> items,
        boolean hasNext,
        String nextCursor
) {
    public sealed interface Item permits RecipeItem, DatasetItem, QnaItem, RecruitItem {
        Long activityId();

        ActivityType type();

        String title();

        String description();

        Boolean isPublic();

        Boolean isMyPageVisible();

        LocalDateTime createdAt();
    }

    public record RecipeItem(
            Long activityId,
            ActivityType type,
            String title,
            String description,
            @PresignedUrl String thumbnailUrl,
            Boolean isPublic,
            Boolean isMyPageVisible,
            Integer likeCount,
            Integer commentCount,
            Integer forkCount,
            LocalDateTime createdAt
    ) implements Item {
        public static RecipeItem from(Recipe recipe, Integer commentCount) {
            return new RecipeItem(
                    recipe.getRecipeId(),
                    ActivityType.RECIPE,
                    recipe.getTitle(),
                    recipe.getShortDescription(),
                    recipe.getThumbnailUrl(),
                    recipe.getIsPublic(),
                    isShownOnProfile(recipe.getIsPublic(), recipe.getIsMyPageVisible()),
                    recipe.getLikeCount(),
                    commentCount,
                    recipe.getForkCount(),
                    recipe.getCreatedAt()
            );
        }
    }

    public record DatasetItem(
            Long activityId,
            ActivityType type,
            String title,
            String description,
            @PresignedUrl String thumbnailUrl,
            Boolean isPublic,
            Boolean isMyPageVisible,
            Integer likeCount,
            Integer downloadCount,
            LocalDateTime createdAt
    ) implements Item {
        public static DatasetItem from(Dataset dataset) {
            return new DatasetItem(
                    dataset.getDatasetId(),
                    ActivityType.DATASET,
                    dataset.getTitle(),
                    dataset.getDescription(),
                    dataset.getBannerImageUrl(),
                    dataset.getIsPublic(),
                    isShownOnProfile(dataset.getIsPublic(), dataset.getIsMyPageVisible()),
                    dataset.getLikeCount(),
                    dataset.getDownloadCount(),
                    dataset.getCreatedAt()
            );
        }
    }

    public record QnaItem(
            Long activityId,
            ActivityType type,
            String title,
            String description,
            Boolean isPublic,
            Boolean isMyPageVisible,
            Integer commentCount,
            LocalDateTime createdAt
    ) implements Item {
        public static QnaItem from(ExpertQnA qna) {
            return new QnaItem(
                    qna.getExpertQnAId(),
                    ActivityType.QNA,
                    qna.getTitle(),
                    qna.getContent(),
                    qna.getIsPublic(),
                    isShownOnProfile(qna.getIsPublic(), qna.getIsMyPageVisible()),
                    qna.getAnswerCount(),
                    qna.getCreatedAt()
            );
        }
    }

    public record RecruitItem(
            Long activityId,
            ActivityType type,
            String title,
            String description,
            Boolean isPublic,
            Boolean isMyPageVisible,
            Integer commentCount,
            LocalDateTime createdAt
    ) implements Item {
        public static RecruitItem from(Recruit recruit, Integer applicationCount) {
            return new RecruitItem(
                    recruit.getRecruitId(),
                    ActivityType.RECRUITMENT,
                    recruit.getJobTitle(),
                    recruit.getDescription(),
                    recruit.getIsPublic(),
                    isShownOnProfile(recruit.getIsPublic(), recruit.getIsMyPageVisible()),
                    applicationCount,
                    recruit.getCreatedAt()
            );
        }
    }

    /**
     * 공개 프로필에 실제로 표시되는지. 공개 프로필 조회 조건(isPublic = true AND isMyPageVisible = true)과 같은 기준이며,
     * 비공개 글(포크 사본 등)은 저장된 표시 설정과 무관하게 false다.
     */
    private static Boolean isShownOnProfile(Boolean isPublic, Boolean isMyPageVisible) {
        return Boolean.TRUE.equals(isPublic) && Boolean.TRUE.equals(isMyPageVisible);
    }
}
