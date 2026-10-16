package com.nodatanolife.bifusion_be.domain.job.controller;

import com.nodatanolife.bifusion_be.domain.job.dto.response.AugmentedDatasetResponse;
import com.nodatanolife.bifusion_be.domain.job.dto.response.TrainedModelResponse;
import com.nodatanolife.bifusion_be.domain.job.service.AiResultQueryService;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "AI Result", description = "학습·추론 화면에서 고르는 증강 데이터셋·학습 모델 목록 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AiResultController {

    private final AiResultQueryService aiResultQueryService;

    @Operation(
            summary = "증강 데이터셋 목록",
            description = """
                    학습 화면의 증강 데이터 선택 목록입니다. 증강이 완료돼 학습에 쓸 수 있는 결과만 최근에 끝난 순서로 반환합니다.
                    - 항목의 datasetId를 학습 요청의 dataset_id로 그대로 보냅니다. 목록에 있는 항목은 학습 요청에서 거절되지 않습니다(같은 판정 기준).
                    - 표시 예: JOB-015 (40장, 2026-10-16) = jobCode + totalSampleCount + finishedAt
                    - 페이지 없이 전체를 반환하며, 결과가 없으면 빈 배열입니다.

                    주요 오류
                    - 403 P002 프로젝트 멤버 아님"""
    )
    @GetMapping("/projects/{projectId}/augmented-datasets")
    public CommonResponse<List<AugmentedDatasetResponse>> getAugmentedDatasets(
            @Parameter(description = "프로젝트 번호", example = "1") @PathVariable Long projectId,
            @AuthenticationPrincipal CustomOAuth2User principal
    ) {
        return CommonResponse.success(
                aiResultQueryService.getAugmentedDatasets(principal.getUser().getUserId(), projectId));
    }

    @Operation(
            summary = "학습 모델 목록",
            description = """
                    추론 화면의 모델 선택 목록입니다. 프로젝트에서 학습이 끝나 만들어진 모델을 최근에 끝난 순서로 반환합니다.
                    - 항목의 trainedModelId를 추론 요청의 model_id로 그대로 보냅니다.
                    - 모델 이름 필드는 없습니다. 표시 예: 5-shot · JOB-015 (2026-10-16 15:23) = kShot + sourceDatasetJobCode + finishedAt
                    - 페이지 없이 전체를 반환하며, 결과가 없으면 빈 배열입니다.

                    주요 오류
                    - 403 P002 프로젝트 멤버 아님"""
    )
    @GetMapping("/projects/{projectId}/trained-models")
    public CommonResponse<List<TrainedModelResponse>> getTrainedModels(
            @Parameter(description = "프로젝트 번호", example = "1") @PathVariable Long projectId,
            @AuthenticationPrincipal CustomOAuth2User principal
    ) {
        return CommonResponse.success(
                aiResultQueryService.getTrainedModels(principal.getUser().getUserId(), projectId));
    }
}
