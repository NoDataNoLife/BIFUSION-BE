package com.nodatanolife.bifusion_be.expert;

import com.nodatanolife.bifusion_be.domain.expert.dto.request.InspectionCreateRequest;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.ExpertTaskSummaryResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionDetailResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionTargetResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.MyInspectionRequestDetailResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.MyInspectionRequestResponse;
import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertTask;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionProgressStatus;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequest;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionRequestStatus;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;
import com.nodatanolife.bifusion_be.domain.expert.repository.ExpertTaskRepository;
import com.nodatanolife.bifusion_be.domain.expert.repository.InspectionRequestRepository;
import com.nodatanolife.bifusion_be.domain.expert.service.ExpertService;
import com.nodatanolife.bifusion_be.domain.expert.service.ExpertTaskService;
import com.nodatanolife.bifusion_be.domain.expert.service.InspectionResultService;
import com.nodatanolife.bifusion_be.domain.expert.service.InspectionService;
import com.nodatanolife.bifusion_be.domain.file.entity.File;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationConfig;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationParameters;
import com.nodatanolife.bifusion_be.domain.job.entity.Job;
import com.nodatanolife.bifusion_be.domain.job.entity.JobType;
import com.nodatanolife.bifusion_be.domain.job.repository.AugmentationConfigRepository;
import com.nodatanolife.bifusion_be.domain.job.repository.JobRepository;
import com.nodatanolife.bifusion_be.domain.notification.entity.NotificationType;
import com.nodatanolife.bifusion_be.domain.recipe.entity.RecipeListType;
import com.nodatanolife.bifusion_be.domain.recipe.service.RecipeForkService;
import com.nodatanolife.bifusion_be.domain.user.entity.RewardHistory;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import com.nodatanolife.bifusion_be.support.IntegrationTestSupport;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 레시피·업로드 데이터셋 검수, 복제 사본 차단, 스토리지 크기 기준 리워드 범위, 대상 삭제 시 처리,
 * 전문가 목록·상세의 대상 정보, 배정 없이 남은 신청 정리.
 */
class InspectionIntegrationTest extends IntegrationTestSupport {

    private static final long MB = 1024L * 1024;

    @Autowired
    InspectionService inspectionService;
    @Autowired
    InspectionResultService inspectionResultService;
    @Autowired
    ExpertService expertService;
    @Autowired
    ExpertTaskService expertTaskService;
    @Autowired
    ExpertTaskRepository expertTaskRepository;
    @Autowired
    InspectionRequestRepository inspectionRequestRepository;
    @Autowired
    RecipeForkService recipeForkService;
    @Autowired
    JobRepository jobRepository;
    @Autowired
    AugmentationConfigRepository augmentationConfigRepository;

    // ---------- 신청 ----------

    @Test
    void 레시피_검수를_신청하면_전문가에게_배정되고_포인트가_차감_기록된다() {
        User owner = user("owner@test.com", 1000);
        expert("expert@test.com", 0);
        Long recipeId = recipe(owner, null, true);

        InspectionResponse response = inspectionService.createInspection(owner, create(InspectionTargetType.RECIPE, recipeId, 500));

        assertThat(response.inspectionRequestId()).isNotNull();
        assertThat(expertTaskRepository.findAll()).hasSize(1);
        assertThat(pointsOf(owner)).isEqualTo(500);
        assertThat(rewardHistoryAmount(owner, RewardHistory.TYPE_INSPECTION_REQUEST)).isEqualTo(-500);
    }

    @Test
    void 업로드_데이터셋_검수를_신청할_수_있다() {
        User owner = user("owner@test.com", 1000);
        expert("expert@test.com", 0);
        Long datasetId = dataset(owner, true, tempFile(owner, "application/zip", 5 * MB));

        inspectionService.createInspection(owner, create(InspectionTargetType.UPLOADED_DATA, datasetId, 500));

        assertThat(expertTaskRepository.findAll()).hasSize(1);
    }

    @Test
    void 남의_레시피나_삭제된_레시피는_신청할_수_없다() {
        User owner = user("owner@test.com", 1000);
        User other = user("other@test.com", 1000);
        expert("expert@test.com", 0);
        Long recipeId = recipe(owner, null, true);

        assertError(() -> inspectionService.createInspection(other, create(InspectionTargetType.RECIPE, recipeId, 500)),
                ErrorCode.INSPECTION_FORBIDDEN);
        recipeService.deleteRecipe(recipeId, owner.getUserId());
        assertError(() -> inspectionService.createInspection(owner, create(InspectionTargetType.RECIPE, recipeId, 500)),
                ErrorCode.INSPECTION_TARGET_NOT_FOUND);
    }

    @Test
    void 그대로_복제한_레시피는_신청할_수_없고_불러와_다시_등록한_레시피는_신청할_수_있다() {
        User author = user("author@test.com", 0);
        User forker = user("forker@test.com", 2000);
        expert("expert@test.com", 0);
        Long originalId = recipe(author, null, true);
        Long copyId = recipeForkService.forkRecipe(originalId, forker.getUserId()).newRecipeId();
        Long reRegisteredId = recipe(forker, originalId, true);

        assertError(() -> inspectionService.createInspection(forker, create(InspectionTargetType.RECIPE, copyId, 500)),
                ErrorCode.INSPECTION_FORKED_RECIPE_NOT_ALLOWED);
        inspectionService.createInspection(forker, create(InspectionTargetType.RECIPE, reRegisteredId, 500));

        Map<Long, InspectionTargetResponse> targets = inspectionResultService.getRecipeTargets(forker.getUserId(),
                recipeRepositoryFindAll(copyId, reRegisteredId));
        assertThat(targets.get(copyId).canRequest()).isFalse();
        assertThat(targets.get(reRegisteredId).status()).isEqualTo(InspectionRequestStatus.PENDING);
    }

    @Test
    void 레시피는_500에서_3000_사이만_신청할_수_있다() {
        User owner = user("owner@test.com", 5000);
        expert("expert@test.com", 0);
        Long recipeId = recipe(owner, null, true);

        assertError(() -> inspectionService.createInspection(owner, create(InspectionTargetType.RECIPE, recipeId, 499)),
                ErrorCode.INSPECTION_REWARD_OUT_OF_RANGE);
        assertError(() -> inspectionService.createInspection(owner, create(InspectionTargetType.RECIPE, recipeId, 3001)),
                ErrorCode.INSPECTION_REWARD_OUT_OF_RANGE);
        inspectionService.createInspection(owner, create(InspectionTargetType.RECIPE, recipeId, 3000));
    }

    @Test
    void 데이터셋_리워드_범위는_첨부_파일_크기에_비례한다() {
        User owner = user("owner@test.com", 0);

        assertRange(owner, 10 * MB, 500, 3000);
        assertRange(owner, 100 * MB, 1000, 10000);
        assertRange(owner, 200 * MB, 2000, 10000);
    }

    @Test
    void 증강_데이터_리워드_범위는_증강_결과_파일_크기에_비례한다() {
        User owner = user("owner@test.com", 0);
        Long projectId = project(owner);
        Long configId = augmentedConfig(owner, projectId, 50 * MB);

        // 연관 엔티티(Job·User)를 읽으므로 트랜잭션 안에서 조회한다 (실제 호출 경로인 AI 결과 목록 조회와 같다)
        InspectionTargetResponse target = transactionTemplate.execute(status -> inspectionResultService
                .getAugmentedDataTargets(owner.getUserId(), List.of(augmentationConfigRepository.findById(configId).orElseThrow()))
                .get(configId));

        assertThat(target.minRewardPoints()).isEqualTo(500);
        assertThat(target.maxRewardPoints()).isEqualTo(5000);
    }

    @Test
    void 포인트가_부족하면_신청이_막히고_아무것도_남지_않는다() {
        User owner = user("owner@test.com", 100);
        expert("expert@test.com", 0);
        Long recipeId = recipe(owner, null, true);

        assertError(() -> inspectionService.createInspection(owner, create(InspectionTargetType.RECIPE, recipeId, 500)),
                ErrorCode.INSUFFICIENT_POINTS);

        assertThat(inspectionRequestRepository.findAll()).isEmpty();
        assertThat(expertTaskRepository.findAll()).isEmpty();
        assertThat(pointsOf(owner)).isEqualTo(100);
    }

    @Test
    void 레시피_상세와_내_레시피_목록에_본인에게만_신청_정보가_나온다() {
        User owner = user("owner@test.com", 0);
        User other = user("other@test.com", 0);
        Long recipeId = recipe(owner, null, true);

        InspectionTargetResponse mine = recipeService.getRecipeDetail(recipeId, owner.getUserId()).inspectionTarget();
        assertThat(mine.canRequest()).isTrue();
        assertThat(mine.minRewardPoints()).isEqualTo(500);
        assertThat(mine.maxRewardPoints()).isEqualTo(3000);
        assertThat(recipeService.getRecipeDetail(recipeId, other.getUserId()).inspectionTarget()).isNull();
        assertThat(recipeService.getMyRecipes(RecipeListType.MINE, owner.getUserId(), 0, 10).getContent().get(0).inspectionTarget())
                .isNotNull();
    }

    @Test
    void 데이터셋_상세와_내_데이터셋_목록에_본인에게만_신청_정보가_나온다() {
        User owner = user("owner@test.com", 0);
        User other = user("other@test.com", 0);
        Long datasetId = dataset(owner, true, tempFile(owner, "application/zip", MB));

        assertThat(datasetService.getDatasetDetail(datasetId, owner.getUserId()).inspectionTarget().canRequest()).isTrue();
        assertThat(datasetService.getDatasetDetail(datasetId, other.getUserId()).inspectionTarget()).isNull();
        assertThat(datasetService.getMyDatasets(com.nodatanolife.bifusion_be.domain.dataset.entity.DatasetType.UPLOADED,
                owner.getUserId(), 0, 10).getContent().get(0).inspectionTarget()).isNotNull();
    }

    // ---------- 전문가 처리 ----------

    @Test
    void 전문가_목록과_상세에_레시피_검수_대상_정보가_나온다() {
        User owner = user("owner@test.com", 1000);
        User expert = expert("expert@test.com", 0);
        Long recipeId = recipe(owner, null, true);
        inspectionService.createInspection(owner, create(InspectionTargetType.RECIPE, recipeId, 500));
        Long taskId = onlyTask().getExpertTaskId();

        ExpertTaskSummaryResponse summary = expertService.getMyTasks(expert).pending().get(0);
        assertThat(summary.targetType()).isEqualTo(InspectionTargetType.RECIPE);
        assertThat(summary.targetTitle()).isEqualTo("레시피");
        assertThat(summary.projectName()).isNull();

        InspectionDetailResponse detail = inspectionService.getInspectionDetail(expert, taskId);
        assertThat(detail.targetType()).isEqualTo(InspectionTargetType.RECIPE);
        assertThat(detail.reason()).isEqualTo("검수 부탁드립니다");
        assertThat(detail.recipe().recipeId()).isEqualTo(recipeId);
        assertThat(detail.recipe().settings().model()).isEqualTo("sd-1.5");
        assertThat(detail.dataset()).isNull();
        assertThat(detail.sampleImages()).isNull();
    }

    @Test
    void 전문가_목록과_상세에_데이터셋_대표_이미지와_파일_정보가_나온다() {
        User owner = user("owner@test.com", 1000);
        User expert = expert("expert@test.com", 0);
        File image = tempFile(owner, "image/png", MB);
        Long datasetId = dataset(owner, false, image);
        inspectionService.createInspection(owner, create(InspectionTargetType.UPLOADED_DATA, datasetId, 500));

        ExpertTaskSummaryResponse summary = expertService.getMyTasks(expert).pending().get(0);
        assertThat(summary.targetImageUrl()).isEqualTo(image.getS3Key());

        InspectionDetailResponse.DatasetTarget dataset = inspectionService.getInspectionDetail(expert, onlyTask().getExpertTaskId()).dataset();
        assertThat(dataset.datasetId()).isEqualTo(datasetId);
        assertThat(dataset.fileId()).isEqualTo(image.getFileId());
        assertThat(dataset.bannerImageUrl()).isEqualTo(image.getS3Key());
    }

    @Test
    void 레시피_검수를_승인하면_전문가에게_90퍼센트가_지급되고_레시피_승인_알림이_간다() {
        User owner = user("owner@test.com", 1000);
        User expert = expert("expert@test.com", 0);
        Long recipeId = recipe(owner, null, true);
        inspectionService.createInspection(owner, create(InspectionTargetType.RECIPE, recipeId, 1000));

        inspectionService.approveInspection(expert, onlyTask().getExpertTaskId(), "좋습니다");

        assertThat(pointsOf(expert)).isEqualTo(900);
        assertThat(rewardHistoryAmount(expert, RewardHistory.TYPE_EXPERT_TASK_REWARD)).isEqualTo(900);
        assertThat(notificationTypes(owner)).contains(NotificationType.RECIPE_APPROVED.name());
        assertThat(recipeService.getRecipeDetail(recipeId, owner.getUserId()).isExpertVerified()).isTrue();
    }

    @Test
    void 데이터셋_검수를_거절하면_데이터셋_거절_알림이_간다() {
        User owner = user("owner@test.com", 1000);
        User expert = expert("expert@test.com", 0);
        Long datasetId = dataset(owner, true, tempFile(owner, "application/zip", MB));
        inspectionService.createInspection(owner, create(InspectionTargetType.UPLOADED_DATA, datasetId, 500));

        inspectionService.rejectInspection(expert, onlyTask().getExpertTaskId(), "라벨이 틀렸습니다");

        assertThat(notificationTypes(owner)).contains(NotificationType.DATASET_VERIFY_REJECTED.name());
    }

    @Test
    void 레시피_데이터셋_검수에는_이미지_코멘트를_달_수_없다() {
        User owner = user("owner@test.com", 1000);
        User expert = expert("expert@test.com", 0);
        Long recipeId = recipe(owner, null, true);
        inspectionService.createInspection(owner, create(InspectionTargetType.RECIPE, recipeId, 500));

        assertError(() -> inspectionService.saveImageComment(expert, onlyTask().getExpertTaskId(), 1L, "코멘트"),
                ErrorCode.INSPECTION_IMAGE_NOT_FOUND);
    }

    // ---------- 신청자 목록·상세 ----------

    @Test
    void 내가_요청한_검수_목록은_진행_상태와_대상_정보를_담고_필터와_무관한_상태별_건수를_준다() {
        User owner = user("owner@test.com", 3000);
        User expert = expert("expert@test.com", 0);
        Long recipeId = recipe(owner, null, true);
        Long datasetId = dataset(owner, true, tempFile(owner, "application/zip", MB));
        Long configId = augmentedConfig(owner, project(owner), 50 * MB);
        Long recipeRequestId = inspectionService.createInspection(owner, create(InspectionTargetType.RECIPE, recipeId, 500)).inspectionRequestId();
        inspectionService.createInspection(owner, create(InspectionTargetType.UPLOADED_DATA, datasetId, 500));
        inspectionService.createInspection(owner, create(InspectionTargetType.AUGMENTED_DATA, configId, 500));
        inspectionService.approveInspection(expert, taskOf(InspectionTargetType.RECIPE).getExpertTaskId(), "좋습니다");
        expertTaskService.startTask(expert, taskOf(InspectionTargetType.UPLOADED_DATA).getExpertTaskId());

        Page<MyInspectionRequestResponse> all = inspectionService.getMyRequests(owner, null, null, null, 0, 10);

        assertThat(inspectionService.getMyRequestCounts(owner)).containsExactlyInAnyOrderEntriesOf(Map.of(
                InspectionProgressStatus.PENDING, 1L, InspectionProgressStatus.IN_PROGRESS, 1L,
                InspectionProgressStatus.APPROVED, 1L, InspectionProgressStatus.REJECTED, 0L));
        assertThat(all.getTotalElements()).isEqualTo(3);
        // 신청일 최신순
        assertThat(all.getContent()).extracting(r -> r.target().type()).containsExactly(
                InspectionTargetType.AUGMENTED_DATA, InspectionTargetType.UPLOADED_DATA, InspectionTargetType.RECIPE);

        MyInspectionRequestResponse augmented = all.getContent().get(0);
        assertThat(augmented.status()).isEqualTo(InspectionProgressStatus.PENDING);
        assertThat(augmented.target().id()).isEqualTo(configId);
        assertThat(augmented.target().jobId()).isNotNull();
        assertThat(augmented.target().name()).isEqualTo("프로젝트");
        assertThat(augmented.target().imageUrl()).isEqualTo("ml-results/preview.png");
        assertThat(augmented.expert().nickname()).isEqualTo(reload(expert).getNickname());
        assertThat(augmented.result()).isNull();
        assertThat(augmented.completedAt()).isNull();

        MyInspectionRequestResponse recipe = all.getContent().get(2);
        assertThat(recipe.inspectionRequestId()).isEqualTo(recipeRequestId);
        assertThat(recipe.status()).isEqualTo(InspectionProgressStatus.APPROVED);
        assertThat(recipe.target().name()).isEqualTo("레시피");
        assertThat(recipe.result().comment()).isEqualTo("좋습니다");
        assertThat(recipe.completedAt()).isNotNull();

        assertThat(inspectionService.getMyRequests(owner, null, InspectionProgressStatus.IN_PROGRESS, null, 0, 10)
                .getContent()).extracting(r -> r.target().type()).containsExactly(InspectionTargetType.UPLOADED_DATA);
        assertThat(inspectionService.getMyRequests(owner, null, InspectionProgressStatus.PENDING, null, 0, 10)
                .getContent()).extracting(r -> r.target().type()).containsExactly(InspectionTargetType.AUGMENTED_DATA);
        assertThat(inspectionService.getMyRequests(owner, InspectionTargetType.RECIPE, null, null, 0, 10)
                .getContent()).extracting(MyInspectionRequestResponse::inspectionRequestId).containsExactly(recipeRequestId);
        assertThat(inspectionService.getMyRequests(owner, null, null, "  데이터 ", 0, 10)
                .getContent()).extracting(r -> r.target().type()).containsExactly(InspectionTargetType.UPLOADED_DATA);
        assertThat(inspectionService.getMyRequests(owner, null, null, "프로젝", 0, 10)
                .getContent()).extracting(r -> r.target().type()).containsExactly(InspectionTargetType.AUGMENTED_DATA);

        Page<MyInspectionRequestResponse> firstPage = inspectionService.getMyRequests(owner, null, null, null, 0, 2);
        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }

    @Test
    void 내가_요청한_검수에는_취소한_신청과_남의_신청이_나오지_않고_반려_사유와_삭제된_대상이_보인다() {
        User owner = user("owner@test.com", 2000);
        User other = user("other@test.com", 1000);
        User expert = expert("expert@test.com", 0);
        Long cancelledRecipe = recipe(owner, null, true);
        Long rejectedRecipe = recipe(owner, null, true);
        Long cancelledId = inspectionService.createInspection(owner, create(InspectionTargetType.RECIPE, cancelledRecipe, 500)).inspectionRequestId();
        inspectionService.cancelInspection(owner, cancelledId);
        Long rejectedId = inspectionService.createInspection(owner, create(InspectionTargetType.RECIPE, rejectedRecipe, 500)).inspectionRequestId();
        inspectionService.rejectInspection(expert, onlyTask().getExpertTaskId(), "설명이 부족합니다");
        recipeService.deleteRecipe(rejectedRecipe, owner.getUserId());
        inspectionService.createInspection(other, create(InspectionTargetType.RECIPE, recipe(other, null, true), 500));

        Page<MyInspectionRequestResponse> list = inspectionService.getMyRequests(owner, null, null, null, 0, 10);
        assertThat(list.getContent()).extracting(MyInspectionRequestResponse::inspectionRequestId).containsExactly(rejectedId);
        assertThat(inspectionService.getMyRequestCounts(owner).get(InspectionProgressStatus.REJECTED)).isEqualTo(1L);
        assertThat(inspectionService.getMyRequestCounts(owner).get(InspectionProgressStatus.PENDING)).isZero();

        MyInspectionRequestDetailResponse detail = inspectionService.getMyRequestDetail(owner, rejectedId);
        assertThat(detail.status()).isEqualTo(InspectionProgressStatus.REJECTED);
        assertThat(detail.result().comment()).isEqualTo("설명이 부족합니다");
        assertThat(detail.reason()).isEqualTo("검수 부탁드립니다");
        assertThat(detail.rewardPoints()).isEqualTo(500);
        assertThat(detail.target().deleted()).isTrue();
        assertThat(detail.target().name()).isEqualTo("레시피");

        assertError(() -> inspectionService.getMyRequestDetail(other, rejectedId), ErrorCode.INSPECTION_NOT_FOUND);
        assertError(() -> inspectionService.getMyRequestDetail(owner, cancelledId), ErrorCode.INSPECTION_NOT_FOUND);
    }

    // ---------- 대상 삭제 ----------

    @Test
    void 전문가가_시작하기_전이면_레시피를_삭제할_때_신청이_취소되고_환불된다() {
        User owner = user("owner@test.com", 1000);
        expert("expert@test.com", 0);
        Long recipeId = recipe(owner, null, true);
        inspectionService.createInspection(owner, create(InspectionTargetType.RECIPE, recipeId, 500));

        recipeService.deleteRecipe(recipeId, owner.getUserId());

        assertThat(inspectionRequestRepository.findAll().get(0).getStatus()).isEqualTo(InspectionRequestStatus.CANCELLED);
        assertThat(expertTaskRepository.findAll()).isEmpty();
        assertThat(pointsOf(owner)).isEqualTo(1000);
        assertThat(rewardHistoryAmount(owner, RewardHistory.TYPE_INSPECTION_REFUND)).isEqualTo(500);
    }

    @Test
    void 전문가가_검수를_시작했으면_레시피와_데이터셋을_삭제할_수_없다() {
        User owner = user("owner@test.com", 2000);
        User expert = expert("expert@test.com", 0);
        Long recipeId = recipe(owner, null, true);
        Long datasetId = dataset(owner, true, tempFile(owner, "application/zip", MB));
        inspectionService.createInspection(owner, create(InspectionTargetType.RECIPE, recipeId, 500));
        inspectionService.createInspection(owner, create(InspectionTargetType.UPLOADED_DATA, datasetId, 500));
        expertTaskRepository.findAll().forEach(task -> expertTaskService.startTask(expert, task.getExpertTaskId()));

        assertError(() -> recipeService.deleteRecipe(recipeId, owner.getUserId()), ErrorCode.INSPECTION_IN_PROGRESS_TARGET_DELETE);
        assertError(() -> datasetService.deleteDataset(owner.getUserId(), datasetId), ErrorCode.INSPECTION_IN_PROGRESS_TARGET_DELETE);
    }

    // ---------- 배정 없이 남은 신청 정리 ----------

    @Test
    void 배정_없이_대기_중인_신청은_취소_환불하고_배정된_신청은_건드리지_않는다() {
        User owner = user("owner@test.com", 1000);
        expert("expert@test.com", 0);
        Long assignedRecipe = recipe(owner, null, true);
        inspectionService.createInspection(owner, create(InspectionTargetType.RECIPE, assignedRecipe, 500));
        InspectionRequest orphan = inspectionRequestRepository.save(
                InspectionRequest.create(owner, InspectionTargetType.RECIPE, recipe(owner, null, true), "예전 신청", 300));

        List<Long> ids = inspectionRequestRepository.findIdsWithoutTaskByStatus(InspectionRequestStatus.PENDING);
        assertThat(ids).containsExactly(orphan.getInspectionRequestId());
        assertThat(inspectionService.cancelUnassigned(orphan.getInspectionRequestId())).isTrue();

        assertThat(inspectionRequestRepository.findById(orphan.getInspectionRequestId()).orElseThrow().getStatus())
                .isEqualTo(InspectionRequestStatus.CANCELLED);
        assertThat(pointsOf(owner)).isEqualTo(500 + 300);
        assertThat(inspectionService.cancelUnassigned(orphan.getInspectionRequestId())).isFalse();
    }

    // ---------- helpers ----------

    private InspectionCreateRequest create(InspectionTargetType targetType, Long targetId, int rewardPoints) {
        return request(InspectionCreateRequest.class, Map.of(
                "targetType", targetType, "targetId", targetId, "reason", "검수 부탁드립니다", "rewardPoints", rewardPoints));
    }

    private ExpertTask onlyTask() {
        List<ExpertTask> tasks = expertTaskRepository.findAll();
        assertThat(tasks).hasSize(1);
        return tasks.get(0);
    }

    private ExpertTask taskOf(InspectionTargetType targetType) {
        return transactionTemplate.execute(status -> expertTaskRepository.findAll().stream()
                .filter(t -> t.getInspectionRequest().getTargetType() == targetType)
                .findFirst().orElseThrow());
    }

    private void assertRange(User owner, long fileSize, int min, int max) {
        Long datasetId = dataset(owner, true, tempFile(owner, "application/zip", fileSize));
        InspectionTargetResponse target = datasetService.getDatasetDetail(datasetId, owner.getUserId()).inspectionTarget();
        assertThat(target.minRewardPoints()).as("min for %d bytes", fileSize).isEqualTo(min);
        assertThat(target.maxRewardPoints()).as("max for %d bytes", fileSize).isEqualTo(max);
    }

    private Long augmentedConfig(User owner, Long projectId, long datasetFileSize) {
        Job job = jobRepository.save(Job.create(projectRepository.findById(projectId).orElseThrow(), owner,
                JobType.AUGMENTATION, 1L, "idem-" + System.nanoTime()));
        AugmentationConfig config = AugmentationConfig.create(job, new AugmentationParameters(10, 0.3, 400, 50, 1L),
                List.of(1L), List.of(2L));
        config.recordResult("ds-1", "ml-results/dataset.zip", "ml-results/preview.png", 20, 22);
        AugmentationConfig saved = augmentationConfigRepository.save(config);
        fileRepository.save(File.createJobResult(owner, job.getJobId(), "ml-results/dataset.zip", "application/zip", datasetFileSize));
        return saved.getAugmentationConfigId();
    }

    private List<com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe> recipeRepositoryFindAll(Long... ids) {
        return recipeRepository.findAllById(List.of(ids));
    }

    private List<String> notificationTypes(User user) {
        return jdbc.queryForList("SELECT type FROM notification WHERE user_id = ?", String.class, user.getUserId());
    }

    private static void assertError(ThrowableAssert.ThrowingCallable call, ErrorCode errorCode) {
        assertThatThrownBy(call).isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(errorCode);
    }
}
