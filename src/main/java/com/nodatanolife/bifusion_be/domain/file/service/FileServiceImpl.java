package com.nodatanolife.bifusion_be.domain.file.service;

import com.nodatanolife.bifusion_be.domain.dataset.entity.Dataset;
import com.nodatanolife.bifusion_be.domain.dataset.repository.DatasetRepository;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;
import com.nodatanolife.bifusion_be.domain.expert.repository.ExpertTaskRepository;
import com.nodatanolife.bifusion_be.domain.file.dto.response.FileDownloadResponse;
import com.nodatanolife.bifusion_be.domain.file.dto.response.TempFileUploadResponse;
import com.nodatanolife.bifusion_be.domain.file.entity.File;
import com.nodatanolife.bifusion_be.domain.file.repository.FileRepository;
import com.nodatanolife.bifusion_be.domain.job.repository.JobRepository;
import com.nodatanolife.bifusion_be.domain.project.repository.ProjectMemberRepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FileServiceImpl implements FileService {

    private static final int DOWNLOAD_URL_EXPIRE_SECONDS = 300;

    private final S3Service s3Service;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final DatasetRepository datasetRepository;
    private final JobRepository jobRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ExpertTaskRepository expertTaskRepository;

    /**
     * 임시 파일 다건 업로드
     * S3에 파일 업로드 후 File 엔티티 저장
     */
    @Override
    public List<TempFileUploadResponse> uploadTempFiles(Long userId, List<MultipartFile> files) {
        User user = userRepository.getReferenceById(userId);

        return files.stream()
                .map(file -> uploadSingleTempFile(user, file))
                .toList();
    }

    /**
     * 회원 탈퇴 시 파일 익명화
     * 유저 소유 파일의 owner를 탈퇴 계정으로 변경
     */
    @Override
    public void anonymizeByUser(User user, User deletedUser) {
        fileRepository.findAllByUser(user)
                .forEach(f -> f.anonymize(deletedUser));
    }

    @Override
    @Transactional(readOnly = true)
    public FileDownloadResponse getDownloadUrl(Long userId, Long fileId) {
        File file = fileRepository.findById(fileId)
                .filter(found -> canDownload(userId, found))
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        String presignedUrl = s3Service.toPresignedUrl(
                file.getS3Key(), Duration.ofSeconds(DOWNLOAD_URL_EXPIRE_SECONDS));
        if (presignedUrl == null || presignedUrl.isBlank()) {
            throw new BusinessException(ErrorCode.FILE_DOWNLOAD_FAILED);
        }

        return FileDownloadResponse.from(file, presignedUrl, DOWNLOAD_URL_EXPIRE_SECONDS);
    }

    /**
     * 파일 종류별 다운로드 권한.
     * - Job 입력·결과 파일: 그 Job이 속한 프로젝트의 멤버
     * - 데이터셋 첨부 파일: 공개 데이터셋이면 누구나, 비공개면 작성자와 그 데이터셋 검수를 맡은 전문가.
     *   삭제된 데이터셋은 S3 객체를 지우므로 누구도 받을 수 없다 (그 파일을 쓰는 데이터셋이 모두 삭제됐으면 404).
     * - 그 외(임시 업로드 등): 업로드한 본인
     */
    private boolean canDownload(Long userId, File file) {
        if (File.TARGET_TYPE_JOB.equals(file.getTargetType()) || File.TARGET_TYPE_JOB_RESULT.equals(file.getTargetType())) {
            return file.getTargetId() != null && jobRepository.findById(file.getTargetId())
                    .map(job -> projectMemberRepository.existsByProjectProjectIdAndUserUserId(
                            job.getProject().getProjectId(), userId))
                    .orElse(false);
        }

        List<Dataset> datasets = datasetRepository.findAllByFile(file);
        if (!datasets.isEmpty()) {
            List<Dataset> live = datasets.stream().filter(dataset -> !dataset.isDeleted()).toList();
            boolean publicOrOwned = live.stream().anyMatch(dataset ->
                    Boolean.TRUE.equals(dataset.getIsPublic()) || dataset.getUser().getUserId().equals(userId));
            return publicOrOwned || (!live.isEmpty() && expertTaskRepository.existsAssignedTo(userId,
                    InspectionTargetType.UPLOADED_DATA, live.stream().map(Dataset::getDatasetId).toList()));
        }

        return file.getUser().getUserId().equals(userId);
    }

    /**
     * 임시 파일 단건 업로드
     * S3 temp/ 경로에 업로드 후 File 엔티티 생성
     */
    private TempFileUploadResponse uploadSingleTempFile(User user, MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String nameWithoutExt = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(0, originalFilename.lastIndexOf("."))
                : originalFilename;

        String filename = S3Service.buildUniqueFilename(originalFilename);
        String s3Key = "temp/" + filename;
        String url = s3Service.uploadFile(file, "temp", filename);

        File tempFile = File.createTemp(user, url, s3Key, nameWithoutExt, file.getContentType(), file.getSize());
        fileRepository.save(tempFile);

        return TempFileUploadResponse.from(tempFile);
    }
}