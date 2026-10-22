package com.nodatanolife.bifusion_be.domain.job.service;

import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionTargetResponse;
import com.nodatanolife.bifusion_be.domain.expert.service.InspectionResultService;
import com.nodatanolife.bifusion_be.domain.job.dto.response.AugmentedDatasetResponse;
import com.nodatanolife.bifusion_be.domain.job.dto.response.TrainedModelResponse;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationConfig;
import com.nodatanolife.bifusion_be.domain.job.entity.JobStatus;
import com.nodatanolife.bifusion_be.domain.job.entity.TrainedModel;
import com.nodatanolife.bifusion_be.domain.job.entity.TrainingConfig;
import com.nodatanolife.bifusion_be.domain.job.repository.AugmentationConfigRepository;
import com.nodatanolife.bifusion_be.domain.job.repository.TrainedModelRepository;
import com.nodatanolife.bifusion_be.domain.job.repository.TrainingConfigRepository;
import com.nodatanolife.bifusion_be.domain.project.repository.ProjectMemberRepository;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiResultQueryServiceImpl implements AiResultQueryService {

    private final AugmentationConfigRepository augmentationConfigRepository;
    private final TrainedModelRepository trainedModelRepository;
    private final TrainingConfigRepository trainingConfigRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final InspectionResultService inspectionResultService;

    /**
     * 쿼리 1번: SUCCESS 증강 결과를 Job과 함께 조회하고, 학습 가능 여부는 isReadyForTraining()으로 판정한다.
     * 쿼리 2번: 목록 항목들의 검수 신청을 한 번에 조회해 검수 신청 대상 정보(inspectionTarget)를 붙인다.
     */
    @Override
    public List<AugmentedDatasetResponse> getAugmentedDatasets(Long userId, Long projectId) {
        validateProjectMember(projectId, userId);
        List<AugmentationConfig> configs = augmentationConfigRepository
                .findAllWithJobByProjectIdAndStatus(projectId, JobStatus.SUCCESS).stream()
                .filter(AugmentationConfig::isReadyForTraining)
                .toList();
        Map<Long, InspectionTargetResponse> targets = inspectionResultService.getAugmentedDataTargets(userId, configs);
        return configs.stream()
                .map(config -> AugmentedDatasetResponse.from(config, targets.get(config.getAugmentationConfigId())))
                .toList();
    }

    /**
     * 쿼리 2번: 모델+학습 Job, 학습 설정+원본 증강 Config·Job.
     * TrainedModel은 TrainingConfig와 직접 연관이 없어(학습 Job을 통해서만 이어짐) 학습 Job id로 따로 조회한다.
     */
    @Override
    public List<TrainedModelResponse> getTrainedModels(Long userId, Long projectId) {
        validateProjectMember(projectId, userId);
        List<TrainedModel> models = trainedModelRepository.findAllWithTrainingJobByProjectId(projectId);
        if (models.isEmpty()) {
            return List.of();
        }
        List<Long> trainingJobIds = models.stream()
                .map(model -> model.getTrainingJob().getJobId())
                .toList();
        Map<Long, TrainingConfig> configByJobId = trainingConfigRepository.findAllWithSourceByJobIdIn(trainingJobIds)
                .stream()
                .collect(Collectors.toMap(config -> config.getJob().getJobId(), Function.identity()));

        return models.stream()
                .map(model -> TrainedModelResponse.from(model, configByJobId.get(model.getTrainingJob().getJobId())))
                .toList();
    }

    private void validateProjectMember(Long projectId, Long userId) {
        if (!projectMemberRepository.existsByProjectProjectIdAndUserUserId(projectId, userId)) {
            throw new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED);
        }
    }
}
