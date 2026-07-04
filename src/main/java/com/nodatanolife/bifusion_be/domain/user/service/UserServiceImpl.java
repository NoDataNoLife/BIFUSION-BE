package com.nodatanolife.bifusion_be.domain.user.service;


import com.nodatanolife.bifusion_be.domain.user.dto.response.*;
import com.nodatanolife.bifusion_be.domain.user.dto.response.WebsiteUpdateResponse;
import com.nodatanolife.bifusion_be.domain.user.entity.SubscriptionTier;
import com.nodatanolife.bifusion_be.domain.user.entity.Subscription;
import com.nodatanolife.bifusion_be.domain.user.repository.SubscriptionRepository;
import org.springframework.web.multipart.MultipartFile;
import com.nodatanolife.bifusion_be.domain.activityLog.service.ActivityLogService;
import com.nodatanolife.bifusion_be.domain.auth.service.AuthService;
import com.nodatanolife.bifusion_be.domain.dataset.service.DatasetService;
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
import com.nodatanolife.bifusion_be.domain.job.service.JobCommentService;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final SubscriptionRepository subscriptionRepository;
    // 인증
    private final AuthService authService;

    // 삭제 대상 서비스
    private final SubscriptionService subscriptionService;
    private final UsageHistoryService usageHistoryService;
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
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        return UserResponse.from(user, expertService.resolveStatus(user));
    }

    /**
     * 마이페이지 기본 정보 조회
     * userId 로 유저의 프로필 기본 정보를 반환
     */
    @Override
    @Transactional(readOnly = true)
    public MyPageBasicResponse getMyPageBasic(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        return MyPageBasicResponse.from(user);
    }

    /**
     * 닉네임 수정
     * 유저 닉네임 변경 후 변경된 정보 반환
     */
    @Override
    public NicknameUpdateResponse updateNickname(Long userId, String nickname) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
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
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        user.updateBio(bio);
        return BioUpdateResponse.from(user);
    }

    /**
     * 위치 수정
     * 유저 마이페이지 위치 변경 후 변경된 정보 반환
     */
    @Override
    public LocationUpdateResponse updateLocation(Long userId, String location) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        user.updateLocation(location);
        return LocationUpdateResponse.from(user);
    }

    /**
     * 웹사이트 수정
     * 유저 마이페이지 웹사이트 변경 후 변경된 정보 반환
     */
    @Override
    public WebsiteUpdateResponse updateWebsiteUrl(Long userId, String websiteUrl) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        user.updateWebsiteUrl(websiteUrl);
        return WebsiteUpdateResponse.from(user);
    }


    /**
     * 프로필사진 수정
     */
    @Override
    public ProfileImageUpdateResponse updateProfileImage(Long userId, MultipartFile image) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        String imageUrl = s3Service.uploadFile(image, "profile");
        user.updateProfileImageUrl(imageUrl);
        return ProfileImageUpdateResponse.from(user);
    }

    /**
     * 플랜 변경
     */
    @Override
    @Transactional
    public PlanChangeResponse changePlan(Long userId, SubscriptionTier newTier) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        int oldLimit = user.getPlanType().getMaxTokens();
        int newLimit = newTier.getMaxTokens();
        long updatedTokens = user.getRemainingTokens() + (newLimit - oldLimit);

        Subscription history = user.changePlan(newTier, updatedTokens);

        subscriptionRepository.save(history);
        return PlanChangeResponse.of(newTier, "플랜 변경이 완료되었습니다.");
    }

    /**
     * 온보딩 완료 처리
     * 최초 로그인 후 추가 정보 입력 시 호출
     */
    @Override
    public UserResponse onboard(Long userId, OnboardingRequest request) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

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
     * 2. 개인 기록 삭제 (Subscription, UsageHistory 등)
     * 3. 공개 콘텐츠 익명화 (Recipe, Dataset 등)
     * 4. User hard delete
     */
    @Override
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        User deletedUser = userRepository.findByEmail(deletedUserEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELETED_USER_NOT_FOUND));

        // 1. 인증 정보 삭제
        authService.deleteByUser(user);

        // 2. 개인 기록 삭제
        subscriptionService.deleteByUser(user);
        usageHistoryService.deleteByUser(user);
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


