package com.nodatanolife.bifusion_be.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nodatanolife.bifusion_be.domain.dataset.dto.request.DatasetCreateRequest;
import com.nodatanolife.bifusion_be.domain.dataset.service.DatasetService;
import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertApplication;
import com.nodatanolife.bifusion_be.domain.expert.repository.ExpertApplicationRepository;
import com.nodatanolife.bifusion_be.domain.file.entity.File;
import com.nodatanolife.bifusion_be.domain.file.repository.FileRepository;
import com.nodatanolife.bifusion_be.domain.file.service.S3Service;
import com.nodatanolife.bifusion_be.domain.job.ml.MlClient;
import com.nodatanolife.bifusion_be.domain.project.dto.request.ProjectCreateRequest;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectMember;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectRole;
import com.nodatanolife.bifusion_be.domain.project.repository.ProjectMemberRepository;
import com.nodatanolife.bifusion_be.domain.project.repository.ProjectRepository;
import com.nodatanolife.bifusion_be.domain.project.service.ProjectService;
import com.nodatanolife.bifusion_be.domain.recipe.dto.request.RecipeRegisterRequest;
import com.nodatanolife.bifusion_be.domain.recipe.service.RecipeService;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
import com.nodatanolife.bifusion_be.global.mail.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 실제 Postgres에 붙는 서비스 단위 통합 테스트의 공통 설정.
 * 매 테스트 전에 모든 테이블을 비우므로 TEST_DB_URL(테스트 전용 DB)이 있을 때만 실행한다.
 * 개발용 DB(localhost:5432 등)를 TEST_DB_URL로 지정하지 않는다.
 * 예) TEST_DB_URL=jdbc:postgresql://localhost:55432/bifusion TEST_DB_USERNAME=bifusion_test ./gradlew test
 *
 * S3·메일·ML 호출은 가짜로 바꾸고, ML 폴링 스케줄러는 테스트 중 끼어들지 않게 주기를 늘린다.
 */
@SpringBootTest
@ExtendWith(IntegrationTestSupport.RequireTestDatabase.class)
public abstract class IntegrationTestSupport {

    /**
     * TEST_DB_URL이 없으면 하위 테스트 클래스를 건너뛴다 (Spring 컨텍스트를 띄우기 전에 판단).
     * {@code @EnabledIfEnvironmentVariable}은 상속되지 않아 하위 클래스에서 동작하지 않으므로, 상속되는 {@code @ExtendWith}로 건다.
     * 이 조건이 빠지면 개발 DB에 붙어 매 테스트마다 TRUNCATE될 수 있다.
     */
    static class RequireTestDatabase implements ExecutionCondition {
        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
            String url = System.getenv("TEST_DB_URL");
            return url == null || url.isBlank()
                    ? ConditionEvaluationResult.disabled("TEST_DB_URL(테스트 전용 DB)이 없어 통합 테스트를 건너뜀")
                    : ConditionEvaluationResult.enabled("TEST_DB_URL=" + url);
        }
    }

    protected static final String SIGNED_URL = "https://signed.example/file";

    @DynamicPropertySource
    static void testDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("TEST_DB_URL"));
        registry.add("spring.datasource.username", () -> envOrDefault("TEST_DB_USERNAME", "bifusion_test"));
        registry.add("spring.datasource.password", () -> envOrDefault("TEST_DB_PASSWORD", ""));
        registry.add("ml.polling.interval", () -> "1h");
        registry.add("spring.jpa.show-sql", () -> "false");
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }

    @MockitoBean
    protected S3Service s3Service;

    @MockitoBean
    protected MailService mailService;

    @MockitoBean
    protected MlClient mlClient;

    @Autowired
    protected JdbcTemplate jdbc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected ProjectService projectService;
    @Autowired
    protected ProjectRepository projectRepository;
    @Autowired
    protected ProjectMemberRepository projectMemberRepository;
    @Autowired
    protected RecipeService recipeService;
    @Autowired
    protected com.nodatanolife.bifusion_be.domain.recipe.repository.RecipeRepository recipeRepository;
    @Autowired
    protected org.springframework.transaction.support.TransactionTemplate transactionTemplate;
    @Autowired
    protected DatasetService datasetService;
    @Autowired
    protected FileRepository fileRepository;
    @Autowired
    protected ExpertApplicationRepository expertApplicationRepository;

    @BeforeEach
    void cleanDatabaseAndStubs() {
        List<String> tables = jdbc.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname = 'public'", String.class);
        if (!tables.isEmpty()) {
            jdbc.execute("TRUNCATE TABLE " + String.join(", ", tables) + " RESTART IDENTITY CASCADE");
        }
        when(s3Service.toPresignedUrl(anyString(), any(Duration.class))).thenReturn(SIGNED_URL);
        when(s3Service.toPresignedUrl(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(s3Service.uploadBytes(any(), anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
    }

    // ---------- fixtures ----------

    protected User user(String email, int points) {
        User user = User.createOAuthUser(email, email.substring(0, email.indexOf('@')));
        user.onboard(email.substring(0, email.indexOf('@')), null, null);
        user.addPoints(points);
        return userRepository.save(user);
    }

    protected User reload(User user) {
        return userRepository.findById(user.getUserId()).orElseThrow();
    }

    protected int pointsOf(User user) {
        return jdbc.queryForObject("SELECT reward_balance FROM users WHERE user_id = ?", Integer.class, user.getUserId());
    }

    /**
     * 전문가 인증(APPROVED) 사용자. 승인 처리 API가 없어 상태는 DB로 직접 바꾼다.
     */
    protected User expert(String email, int points) {
        User expert = user(email, points);
        ExpertApplication application = expertApplicationRepository.save(ExpertApplication.create(expert, "doc.pdf"));
        jdbc.update("UPDATE expert_application SET status = 'APPROVED' WHERE expert_application_id = ?",
                application.getExpertApplicationId());
        return expert;
    }

    protected Long project(User owner) {
        return projectService.createProject(new ProjectCreateRequest("프로젝트", "설명", true), owner.getEmail()).projectId();
    }

    protected void addMember(Long projectId, User user, ProjectRole role) {
        projectMemberRepository.save(ProjectMember.of(projectRepository.findById(projectId).orElseThrow(), user, role));
    }

    protected Long recipe(User author, Long forkedFromId, boolean isPublic) {
        RecipeRegisterRequest request = new RecipeRegisterRequest("레시피", "설명", "sd-1.5", 30, "euler", 7.5f, 42,
                "512x512", 1, "개요", List.of("특징"), List.of("용도"), forkedFromId, isPublic, null);
        String code = recipeService.registerRecipe(request, author.getUserId()).recipeId();
        return Long.parseLong(code.substring("REC-".length()));
    }

    protected File tempFile(User owner, String fileType, long size) {
        String key = "temp/" + System.nanoTime() + "_file";
        return fileRepository.save(File.createTemp(owner, "https://bucket/" + key, key, "file", fileType, size));
    }

    protected Long dataset(User owner, boolean isPublic, File file) {
        DatasetCreateRequest request = objectMapper.convertValue(Map.of(
                "title", "데이터셋", "description", "설명", "fileId", file.getFileId(), "isPublic", isPublic),
                DatasetCreateRequest.class);
        return datasetService.createDataset(owner, request).datasetId();
    }

    protected <T> T request(Class<T> type, Map<String, ?> fields) {
        return objectMapper.convertValue(fields, type);
    }

    protected int rewardHistoryCount(User user, String type) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM reward_history WHERE user_id = ? AND type = ?",
                Integer.class, user.getUserId(), type);
    }

    protected Integer rewardHistoryAmount(User user, String type) {
        return jdbc.queryForObject("SELECT SUM(amount) FROM reward_history WHERE user_id = ? AND type = ?",
                Integer.class, user.getUserId(), type);
    }
}
