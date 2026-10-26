package com.nodatanolife.bifusion_be.domain.job.service;

import com.nodatanolife.bifusion_be.domain.job.dto.request.AugmentationJobCreateRequest;
import com.nodatanolife.bifusion_be.domain.job.dto.request.InferenceJobCreateRequest;
import com.nodatanolife.bifusion_be.domain.job.dto.request.JobCreateRequest;
import com.nodatanolife.bifusion_be.domain.job.dto.request.TrainingJobCreateRequest;
import com.nodatanolife.bifusion_be.domain.job.dto.response.JobCostEstimateResponse;
import com.nodatanolife.bifusion_be.domain.job.entity.AugmentationParameters;
import com.nodatanolife.bifusion_be.domain.job.entity.TrainingParameters;
import com.nodatanolife.bifusion_be.domain.job.ml.MlProperties;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 실행 전 예상 비용 조회. 실행 요청과 같은 본문을 받아 같은 기본값·범위 검증·계산식(JobCostPolicy)을 쓴다.
 * 프로젝트·파일·모델은 확인하지 않는다 (비용에 영향이 없고, 이미지를 고르기 전에도 부를 수 있게).
 * 추론만 이미지 수가 비용에 들어가므로 fileIds 장수를 검증한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobCostEstimateService {

    private final JobCostPolicy jobCostPolicy;
    private final JobRequestValidator jobRequestValidator;
    private final MlProperties mlProperties;
    private final UserRepository userRepository;

    public JobCostEstimateResponse estimate(Long userId, JobCreateRequest request) {
        int cost = switch (request) {
            case AugmentationJobCreateRequest r -> {
                AugmentationParameters parameters = r.toParameters().withDefaults(mlProperties.defaults().augmentation());
                jobRequestValidator.validateAugmentationParameters(parameters);
                yield jobCostPolicy.augmentation(parameters);
            }
            case TrainingJobCreateRequest r -> {
                TrainingParameters parameters = r.toParameters().withDefaults(mlProperties.defaults().training());
                jobRequestValidator.validateTraining(parameters);
                yield jobCostPolicy.training(parameters);
            }
            case InferenceJobCreateRequest r -> {
                jobRequestValidator.validateInference(r.getFileIds());
                yield jobCostPolicy.inference(r.getFileIds().size());
            }
            default -> throw new BusinessException(ErrorCode.INVALID_REQUEST);
        };
        User user = userRepository.getReferenceById(userId);
        int currentPoints = user.getPoints() == null ? 0 : user.getPoints();
        return JobCostEstimateResponse.of(request.getType(), cost, currentPoints);
    }
}
