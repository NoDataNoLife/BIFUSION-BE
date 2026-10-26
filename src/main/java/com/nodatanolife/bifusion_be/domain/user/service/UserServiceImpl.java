package com.nodatanolife.bifusion_be.domain.user.service;

import com.nodatanolife.bifusion_be.domain.recipe.service.RecipeReviewService;
import com.nodatanolife.bifusion_be.domain.recipe.entity.Recipe;
import com.nodatanolife.bifusion_be.domain.project.dto.response.MyProjectsResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import com.nodatanolife.bifusion_be.domain.user.dto.response.*;
import com.nodatanolife.bifusion_be.domain.user.dto.response.WebsiteUpdateResponse;
import org.springframework.web.multipart.MultipartFile;
import com.nodatanolife.bifusion_be.domain.activityLog.service.ActivityLogService;
import com.nodatanolife.bifusion_be.domain.auth.service.AuthService;
import com.nodatanolife.bifusion_be.domain.dataset.service.DatasetService;
import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertApplicationStatus;
import com.nodatanolife.bifusion_be.domain.expert.service.ExpertService;
import com.nodatanolife.bifusion_be.domain.file.service.FileService;
import com.nodatanolife.bifusion_be.domain.file.service.S3Service;
import com.nodatanolife.bifusion_be.domain.job.service.JobService;
import com.nodatanolife.bifusion_be.domain.notification.service.NotificationService;
import com.nodatanolife.bifusion_be.domain.project.service.ProjectService;
import com.nodatanolife.bifusion_be.domain.qna.service.ExpertQnAService;
import com.nodatanolife.bifusion_be.domain.recipe.service.RecipeService;
import com.nodatanolife.bifusion_be.domain.recruitment.service.RecruitService;
import com.nodatanolife.bifusion_be.domain.user.dto.request.OnboardingRequest;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import com.nodatanolife.bifusion_be.global.exception.business.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final RecipeReviewService recipeReviewService;
    // 인증
    private final AuthService authService;

    // 삭제 대상 서비스
    private final RewardHistoryService rewardHistoryService;
    private final NotificationService notificationService;
    private final ProjectService projectService;
    private final ExpertService expertService;

    // 익명화 대상 서비스
    private final RecipeService recipeService;
    private final DatasetService datasetService;
    private final ExpertQnAService expertQnAService;
    private final RecruitService recruitService;
    private final JobService jobService;
    private final ActivityLogService activityLogService;
    private final FileService fileService;
    private final S3Service s3Service;

    @Value("${deleted-user.email:deleted@bifusion.com}")
    private String deletedUserEmail;

    /**
     * 내 정보 조회
     * JWT 쿠키로 인증된 유저 본인의 프로필 정보 반환
     */
    @Override
    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        User user = userRepository.getReferenceById(userId);
        return UserResponse.from(user, expertService.resolveStatus(user));
    }

    /**
     * 마이페이지 기본 정보 조회
     * userId 로 유저의 프로필 기본 정보를 반환
     */
    @Override
    @Transactional(readOnly = true)
    public MyPageBasicResponse getMyPageBasic(Long userId, int size) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        boolean isExpert = expertService.resolveStatus(user) == ExpertApplicationStatus.APPROVED;

        List<MyPageBasicResponse.PublicActivityResponse> activities = new ArrayList<>();

// 1. RECIPE (쇼케이스)
        List<Recipe> recipes = recipeService.getPublicRecipesByUser(userId);
        Map<Long, Long> commentCounts = recipeReviewService.countCommentsByRecipeIds(
                recipes.stream().map(Recipe::getRecipeId).toList());

        activities.addAll(recipes.stream()
                .map(r -> new MyPageBasicResponse.PublicActivityResponse.RecipeItem(
                        r.getRecipeId(),
                        "RECIPE",
                        r.getTitle(),
                        r.getShortDescription(),
                        r.getThumbnailUrl(),
                        r.getIsPublic(),
                        r.getLikeCount(),
                        commentCounts.getOrDefault(r.getRecipeId(), 0L),
                        r.getForkCount(),
                        r.getCreatedAt()
                )).toList());

        // 2. DATASET
        activities.addAll(datasetService.getPublicDatasetsByUser(userId).stream()
                .map(d -> new MyPageBasicResponse.PublicActivityResponse.DatasetItem(
                        d.getDatasetId(),
                        "DATASET",
                        d.getTitle(),
                        d.getDescription(),
                        null,
                        d.getIsPublic(),
                        d.getLikeCount(),
                        d.getDownloadCount(),
                        d.getCreatedAt()
                )).toList());

        // 3. QNA
        activities.addAll(expertQnAService.getPublicQnasByUser(userId).stream()
                .map(q -> new MyPageBasicResponse.PublicActivityResponse.QnaItem(
                        q.getExpertQnAId(),
                        "QNA",
                        q.getTitle(),
                        q.getContent(),
                        q.getIsPublic(),
                        q.getAnswerCount(), // commentCount로 매핑 (전문가 답변 수)
                        q.getCreatedAt()
                )).toList());

        // 4. RECRUITMENT
        activities.addAll(recruitService.getPublicRecruitsByUser(userId).stream()
                .map(r -> new MyPageBasicResponse.PublicActivityResponse.RecruitItem(
                        r.getRecruitId(),
                        "RECRUITMENT",
                        r.getJobTitle(),
                        r.getDescription(),
                        r.getIsPublic(),
                        0, // 지원자 수 필드/로직에 맞춰 매핑 (없으면 기본값 0)
                        r.getCreatedAt()
                )).toList());

        // 5. PROJECT
        MyProjectsResponse projects = projectService.getMyProjects(user.getEmail(), null, size, true);
        activities.addAll(projects.items().stream()
                .map(item -> new MyPageBasicResponse.PublicActivityResponse.ProjectItem(
                        item.projectId(),
                        "PROJECT",
                        item.title(),
                        null,
                        true, // isPublic (공개 프로젝트)
                        item.bannerImageUrl(),
                        item.role(),
                        item.createdAt()
                )).toList());

        activities.sort((a, b) -> {
            if (a.createdAt() == null && b.createdAt() == null) return 0;
            if (a.createdAt() == null) return 1;
            if (b.createdAt() == null) return -1;
            return b.createdAt().compareTo(a.createdAt());
        });

        List<MyPageBasicResponse.PublicActivityResponse> pagedActivities = activities.stream()
                .limit(size)
                .toList();

        return MyPageBasicResponse.from(user, isExpert, pagedActivities);
    }

    /**
     * 닉네임 수정
     * 유저 닉네임 변경 후 변경된 정보 반환
     */
    @Override
    public NicknameUpdateResponse updateNickname(Long userId, String nickname) {
        User user = userRepository.getReferenceById(userId);
        user.updateNickname(nickname);
        return NicknameUpdateResponse.from(user);
    }


    /**
     * 소개글 수정
     * 유저 마이페이지 소개글 변경 후 변경된 정보 반환
     */
    @Override
    @Transactional
    public BioUpdateResponse updateBio(Long userId, String bio) {
        User user = userRepository.getReferenceById(userId);
        user.updateBio(bio);
        return BioUpdateResponse.from(user);
    }

    /**
     * 위치 수정
     * 유저 마이페이지 위치 변경 후 변경된 정보 반환
     */
    @Override
    public LocationUpdateResponse updateLocation(Long userId, String location) {
        User user = userRepository.getReferenceById(userId);
        user.updateLocation(location);
        return LocationUpdateResponse.from(user);
    }

    /**
     * 웹사이트 수정
     * 유저 마이페이지 웹사이트 변경 후 변경된 정보 반환
     */
    @Override
    public WebsiteUpdateResponse updateWebsiteUrl(Long userId, String websiteUrl) {
        User user = userRepository.getReferenceById(userId);
        user.updateWebsiteUrl(websiteUrl);
        return WebsiteUpdateResponse.from(user);
    }


    /**
     * 프로필사진 수정
     */
    @Override
    public ProfileImageUpdateResponse updateProfileImage(Long userId, MultipartFile image) {
        User user = userRepository.getReferenceById(userId);
        String imageUrl = s3Service.uploadFile(image, "profile");
        user.updateProfileImageUrl(imageUrl);
        return ProfileImageUpdateResponse.from(user);
    }

    /**
     * 온보딩 완료 처리
     * 최초 로그인 후 추가 정보 입력 시 호출
     */
    @Override
    public UserResponse onboard(Long userId, OnboardingRequest request) {
        User user = userRepository.getReferenceById(userId);

        if (Boolean.TRUE.equals(user.getIsOnboarded())) {
            throw new BusinessException(ErrorCode.ALREADY_ONBOARDED);
        }

        user.onboard(
                request.nickname(),
                request.organization(),
                request.contact()
        );
        return UserResponse.from(user, expertService.resolveStatus(user));
    }

    /**
     * 회원 탈퇴
     * 1. 인증 정보 삭제 (RefreshToken, OauthAccount)
     * 2. 개인 기록 삭제 (RewardHistory, Notification 등)
     * 3. 공개 콘텐츠 익명화 (Recipe, Dataset 등)
     * 4. User hard delete
     */
    @Override
    public void withdraw(Long userId) {
        User user = userRepository.getReferenceById(userId);

        User deletedUser = userRepository.findByEmail(deletedUserEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELETED_USER_NOT_FOUND));

        // 1. 인증 정보 삭제
        authService.deleteByUser(user);

        // 2. 개인 기록 삭제
        rewardHistoryService.deleteByUser(user);
        notificationService.deleteByUser(user);
        projectService.deleteByUser(user);
        expertService.deleteApplicationByUser(user);

        // 3. 공개 콘텐츠 익명화
        recipeService.anonymizeByUser(user, deletedUser);
        datasetService.anonymizeByUser(user, deletedUser);
        expertQnAService.anonymizeByUser(user, deletedUser);
        recruitService.anonymizeByUser(user, deletedUser);
        jobService.anonymizeByUser(user, deletedUser);
        activityLogService.anonymizeByUser(user, deletedUser);
        expertService.anonymizeByUser(user, deletedUser);
        fileService.anonymizeByUser(user, deletedUser);

        // 4. User hard delete
        userRepository.delete(user);
    }

    /**
     * 이메일로 유저 검색
     * 이메일 기반으로 유저를 조회하며, 미가입 이메일인 경우 exists: false 반환
     */
    public UserSearchResponse searchByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(UserSearchResponse::found)
                .orElseGet(() -> UserSearchResponse.notFound(email));
    }
}


