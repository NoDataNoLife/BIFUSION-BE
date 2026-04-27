package com.nodatanolife.bifusion_be.domain.expert.service;

import com.nodatanolife.bifusion_be.domain.expert.dto.response.ExpertApplicationResponse;
import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertApplication;
import com.nodatanolife.bifusion_be.domain.expert.repository.ExpertApplicationRepository;
import com.nodatanolife.bifusion_be.domain.expert.repository.ExpertTaskRepository;
import com.nodatanolife.bifusion_be.domain.expert.repository.ExpertVerificationRepository;
import com.nodatanolife.bifusion_be.domain.file.service.S3Service;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpertServiceImpl implements ExpertService {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");
    private static final String S3_PREFIX = "expert-application";

    private final ExpertApplicationRepository expertApplicationRepository;
    private final ExpertTaskRepository expertTaskRepository;
    private final ExpertVerificationRepository expertVerificationRepository;
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

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (expertApplicationRepository.existsByUserAndStatus(user, ExpertApplication.STATUS_APPROVED)) {
            throw new BusinessException(ErrorCode.EXPERT_ALREADY_APPROVED);
        }

        String newFileUrl = s3Service.uploadFile(certificationFile, S3_PREFIX);

        Optional<ExpertApplication> existingPending =
                expertApplicationRepository.findByUserAndStatus(user, ExpertApplication.STATUS_PENDING);

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
