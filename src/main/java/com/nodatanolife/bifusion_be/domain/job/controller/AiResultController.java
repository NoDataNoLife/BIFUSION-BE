package com.nodatanolife.bifusion_be.domain.job.controller;

import com.nodatanolife.bifusion_be.domain.job.dto.response.AugmentedDatasetResponse;
import com.nodatanolife.bifusion_be.domain.job.dto.response.TrainedModelResponse;
import com.nodatanolife.bifusion_be.domain.job.service.AiResultQueryService;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "AI Result", description = "학습·추론에 사용할 증강 데이터셋·학습 모델 선택 목록 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AiResultController {

    private final AiResultQueryService aiResultQueryService;

    @Operation(
            summary = "증강 데이터셋 목록",
            description = "학습에 사용할 수 있는 증강 결과 목록입니다. 항목의 datasetId를 학습 요청의 dataset_id로 사용합니다."
    )
    @GetMapping("/projects/{projectId}/augmented-datasets")
    public CommonResponse<List<AugmentedDatasetResponse>> getAugmentedDatasets(
            @PathVariable Long projectId,
            @AuthenticationPrincipal CustomOAuth2User principal
    ) {
        return CommonResponse.success(
                aiResultQueryService.getAugmentedDatasets(principal.getUser().getUserId(), projectId));
    }

    @Operation(
            summary = "학습 모델 목록",
            description = "추론에 사용할 수 있는 학습 모델 목록입니다. 항목의 trainedModelId를 추론 요청의 model_id로 사용합니다."
    )
    @GetMapping("/projects/{projectId}/trained-models")
    public CommonResponse<List<TrainedModelResponse>> getTrainedModels(
            @PathVariable Long projectId,
            @AuthenticationPrincipal CustomOAuth2User principal
    ) {
        return CommonResponse.success(
                aiResultQueryService.getTrainedModels(principal.getUser().getUserId(), projectId));
    }
}
