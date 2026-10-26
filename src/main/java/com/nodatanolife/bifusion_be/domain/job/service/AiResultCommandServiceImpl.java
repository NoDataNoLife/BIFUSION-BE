package com.nodatanolife.bifusion_be.domain.job.service;

import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;
import com.nodatanolife.bifusion_be.domain.expert.service.InspectionService;
import com.nodatanolife.bifusion_be.domain.file.entity.File;
import com.nodatanolife.bifusion_be.domain.file.repository.FileRepository;
import com.nodatanolife.bifusion_be.domain.file.service.S3ObjectCleaner;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationConfig;
import com.nodatanolife.bifusion_be.domain.job.entity.Job;
import com.nodatanolife.bifusion_be.domain.job.repository.AugmentationConfigRepository;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectMember;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectRole;
import com.nodatanolife.bifusion_be.domain.project.repository.ProjectMemberRepository;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class AiResultCommandServiceImpl implements AiResultCommandService {

    private final AugmentationConfigRepository augmentationConfigRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final FileRepository fileRepository;
    private final InspectionService inspectionService;
    private final S3ObjectCleaner s3ObjectCleaner;

    /**
     * 삭제 대상은 증강 데이터셋 목록에 나오는 항목(isReadyForTraining)이고, 실행한 사람이나 프로젝트 관리자만 지울 수 있다.
     * - 전문가가 시작하기 전인 검수 신청은 자동 취소·환불하고, 이미 시작됐으면 409(INSPECTION_IN_PROGRESS_TARGET_DELETE)로 막는다.
     * - 결과 파일(다운로드 zip·미리보기)은 File 행을 지우고 S3 객체는 커밋 후 지운다. 입력 이미지는 남긴다.
     * - ML 서버의 데이터셋 본체(mlDatasetId)는 지우지 않는다. 이미 만든 학습 모델은 그대로 쓸 수 있다.
     */
    @Override
    public void deleteAugmentedDataset(Long userId, Long projectId, Long datasetId) {
        ProjectMember member = projectMemberRepository.findByProjectProjectIdAndUserUserId(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED));
        AugmentationConfig config = augmentationConfigRepository.findByJobJobId(datasetId)
                .filter(found -> found.getJob().getProject().getProjectId().equals(projectId))
                .filter(AugmentationConfig::isReadyForTraining)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUGMENTED_DATASET_NOT_FOUND));
        Job job = config.getJob();
        if (!job.isOwnedBy(userId) && member.getRole() != ProjectRole.MANAGER) {
            throw new BusinessException(ErrorCode.AUGMENTED_DATASET_DELETE_FORBIDDEN);
        }

        // 검수 신청자는 증강 Job의 소유자다 (신청 시 Job 소유자로 검증)
        inspectionService.cancelForTargetDeletion(job.getUser().getUserId(), InspectionTargetType.AUGMENTED_DATA,
                config.getAugmentationConfigId());

        List<File> resultFiles = fileRepository.findAllByTargetTypeAndTargetId(File.TARGET_TYPE_JOB_RESULT, job.getJobId());
        List<String> keys = Stream.concat(
                resultFiles.stream().map(File::getS3Key),
                Stream.of(config.getDatasetKey(), config.getPreviewKey())
        ).toList();
        config.deleteResult();
        fileRepository.deleteAll(resultFiles);
        s3ObjectCleaner.deleteAfterCommit(keys);
    }
}
