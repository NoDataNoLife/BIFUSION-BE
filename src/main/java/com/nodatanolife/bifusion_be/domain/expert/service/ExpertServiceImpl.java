package com.nodatanolife.bifusion_be.domain.expert.service;

import com.nodatanolife.bifusion_be.domain.expert.dto.response.ExpertApplicationResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.ExpertPerformanceResponse;
import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertApplication;
import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertApplicationStatus;
import com.nodatanolife.bifusion_be.domain.expert.repository.ExpertApplicationRepository;
import com.nodatanolife.bifusion_be.domain.expert.repository.ExpertTaskRepository;
import com.nodatanolife.bifusion_be.domain.expert.repository.ExpertVerificationRepository;
import com.nodatanolife.bifusion_be.domain.file.service.S3Service;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.repository.RewardHistoryRepository;
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpertServiceImpl implements ExpertService {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");
    private static final String S3_PREFIX = "expert-application";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private final ExpertApplicationRepository expertApplicationRepository;
    private final ExpertTaskRepository expertTaskRepository;
    private final ExpertVerificationRepository expertVerificationRepository;
    private final RewardHistoryRepository rewardHistoryRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    /**
     * 회원 탈퇴 시 전문가 신청 내역 삭제
     */
    @Override
    public void deleteApplicationByUser(User user) {
        expertApplicationRepository.deleteByUser(user);
    }

    /**
     * 회원 탈퇴 시 전문가 관련 데이터 익명화
     * ExpertTask, ExpertVerification의 전문가/검토자를 탈퇴 계정으로 변경
     */
    @Override
    public void anonymizeByUser(User user, User deletedUser) {
        expertTaskRepository.findAllByExpert(user)
                .forEach(t -> t.anonymize(deletedUser));
        expertVerificationRepository.findAllByUser(user)
                .forEach(v -> v.anonymize(deletedUser));
        expertVerificationRepository.findAllByReviewedBy(user)
                .forEach(v -> v.anonymizeReviewer(deletedUser));
    }

    /**
     * 전문가 인증 신청
     * - APPROVED 상태면 차단 (409)
     * - PENDING 상태가 이미 있으면 첨부 파일을 새 파일로 덮어쓰기 (어드민 큐에 1건만 유지)
     * - 그 외에는 새 PENDING row 생성
     */
    @Override
    public ExpertApplicationResponse apply(Long userId, MultipartFile certificationFile) {
        validateFile(certificationFile);

        User user = userRepository.getReferenceById(userId);

        if (expertApplicationRepository.existsByUserAndStatus(user, ExpertApplicationStatus.APPROVED)) {
            throw new BusinessException(ErrorCode.EXPERT_ALREADY_APPROVED);
        }

        String newFileUrl = s3Service.uploadFile(certificationFile, S3_PREFIX);

        Optional<ExpertApplication> existingPending =
                expertApplicationRepository.findByUserAndStatus(user, ExpertApplicationStatus.PENDING);

        ExpertApplication target;
        if (existingPending.isPresent()) {
            target = existingPending.get();
            String oldUrl = target.getApplicationDocumentUrl();
            target.replaceDocument(newFileUrl);
            s3Service.deleteFileByUrl(oldUrl);
        } else {
            target = expertApplicationRepository.save(
                    ExpertApplication.create(user, newFileUrl)
            );
        }

        return ExpertApplicationResponse.from(target);
    }

    /**
     * 전문가 심사 진행 상태 판정
     * 가장 최근 신청의 상태를 반환한다. (APPROVED는 종료 상태라 항상 마지막 신청)
     * 신청 내역이 없으면 NONE.
     */
    @Override
    @Transactional(readOnly = true)
    public ExpertApplicationStatus resolveStatus(User user) {
        return expertApplicationRepository.findFirstByUserOrderByAppliedAtDesc(user)
                .map(ExpertApplication::getStatus)
                .orElse(ExpertApplicationStatus.NONE);
    }

    /**
     * 전문가 성과 대시보드 조회
     * 전문가 인증(APPROVED)을 받지 않은 사용자는 조회할 수 없다.
     * - totalReward: RewardHistory 합계
     * - taskStats: ExpertTask 상태별 건수 (PENDING/IN_PROGRESS/COMPLETED 항상 포함)
     * - topPercent: APPROVED 전문가 중 검수 완료 건수 기준 순위를 퍼센타일로 환산
     */
    @Override
    @Transactional(readOnly = true)
    public ExpertPerformanceResponse getMyPerformance(User user) {
        if (!expertApplicationRepository.existsByUserAndStatus(user, ExpertApplicationStatus.APPROVED)) {
            throw new BusinessException(ErrorCode.EXPERT_NOT_CERTIFIED);
        }

        int totalReward = rewardHistoryRepository.sumAmountByUser(user);

        Map<String, Long> taskStats = new LinkedHashMap<>();
        taskStats.put(STATUS_PENDING, 0L);
        taskStats.put(STATUS_IN_PROGRESS, 0L);
        taskStats.put(STATUS_COMPLETED, 0L);
        expertTaskRepository.countGroupedByStatus(user)
                .forEach(row -> taskStats.put(row.getStatus(), row.getCount()));

        long completedCount = taskStats.getOrDefault(STATUS_COMPLETED, 0L);

        BigDecimal avgSatisfaction = expertTaskRepository.findAverageSatisfaction(user);
        Double feedbackSatisfaction = avgSatisfaction == null
                ? null
                : avgSatisfaction.setScale(1, RoundingMode.HALF_UP).doubleValue();

        Integer topPercent = calculateTopPercent(user);

        return new ExpertPerformanceResponse(
                true, totalReward, topPercent, completedCount, feedbackSatisfaction, taskStats);
    }

    private Integer calculateTopPercent(User user) {
        List<Long> approvedExpertIds = expertApplicationRepository
                .findAllByStatus(ExpertApplicationStatus.APPROVED)
                .stream()
                .map(a -> a.getUser().getUserId())
                .distinct()
                .toList();

        if (approvedExpertIds.isEmpty()) {
            return null;
        }

        Map<Long, Long> completedCountByExpert = expertTaskRepository
                .countGroupedByExpertForStatus(STATUS_COMPLETED)
                .stream()
                .collect(Collectors.toMap(
                        ExpertTaskRepository.ExpertCompletionCount::getUserId,
                        ExpertTaskRepository.ExpertCompletionCount::getCount
                ));

        long myCount = completedCountByExpert.getOrDefault(user.getUserId(), 0L);

        long rankedAbove = approvedExpertIds.stream()
                .filter(id -> completedCountByExpert.getOrDefault(id, 0L) > myCount)
                .count();

        long rank = rankedAbove + 1;
        return (int) Math.ceil(rank * 100.0 / approvedExpertIds.size());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.EXPERT_FILE_INVALID);
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.EXPERT_FILE_TOO_LARGE);
        }

        String original = file.getOriginalFilename();
        if (original == null || !original.contains(".")) {
            throw new BusinessException(ErrorCode.EXPERT_FILE_INVALID);
        }
        String ext = original.substring(original.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ErrorCode.EXPERT_FILE_INVALID);
        }
    }
}
