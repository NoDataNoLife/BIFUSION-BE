package com.nodatanolife.bifusion_be.domain.job.controller;

import com.nodatanolife.bifusion_be.domain.job.dto.response.AugmentedDatasetResponse;
import com.nodatanolife.bifusion_be.domain.job.dto.response.TrainedModelResponse;
import com.nodatanolife.bifusion_be.domain.job.service.AiResultCommandService;
import com.nodatanolife.bifusion_be.domain.job.service.AiResultQueryService;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "AI Result", description = "학습·추론 화면에서 고르는 증강 데이터셋·학습 모델 목록, 증강 데이터 삭제 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AiResultController {

    private final AiResultQueryService aiResultQueryService;
    private final AiResultCommandService aiResultCommandService;

    @Operation(
            summary = "증강 데이터셋 목록",
            description = """
                    학습 화면의 증강 데이터 선택 목록입니다. 증강이 완료돼 학습에 쓸 수 있는 결과만 최근에 끝난 순서로 반환합니다.
                    - 항목의 datasetId를 학습 요청의 dataset_id로 그대로 보냅니다. 목록에 있는 항목은 학습 요청에서 거절되지 않습니다(같은 판정 기준).
                    - 표시 예: JOB-015 (40장, 2026-10-16) = jobCode + totalSampleCount + finishedAt
                    - 페이지 없이 전체를 반환하며, 결과가 없으면 빈 배열입니다.
                    - inspectionTarget: 검수 신청(POST /api/v1/inspections)에 쓸 targetType·targetId와 신청 가능 여부(canRequest),
                      가장 최근 신청 상태(status)·결과(result). targetId는 AugmentationConfig ID라 datasetId와 다릅니다.
                      신청 건은 전문가 풀에 올라가고 먼저 시작한 전문가가 맡습니다.

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
            summary = "증강 데이터 삭제",
            description = """
                    증강 데이터셋 목록의 항목을 삭제합니다. 증강 작업을 실행한 사람이나 프로젝트 관리자(MANAGER)만 할 수 있습니다.
                    - 결과 파일(다운로드·미리보기)이 클라우드에서 지워져 스토리지 사용량(GET /users/me/resource-usage)이 그만큼 줄어듭니다.
                    - 삭제 후에는 증강 데이터셋 목록과 학습 선택지에서 빠지고, 작업 결과의 previewUrl·downloadUrl은 null입니다.
                    - 입력 이미지와 작업 기록은 남습니다. 이 데이터로 이미 만든 학습 모델도 그대로 쓸 수 있습니다.
                    - 전문가가 아직 시작하지 않은 검수 신청은 자동으로 취소되고 앤트가 환불됩니다.

                    주요 오류
                    - 403 P002 프로젝트 멤버 아님 / J018 실행한 사람·관리자 아님
                    - 404 J017 증강 데이터 없음(이미 삭제됨 포함)
                    - 409 IS012 전문가가 검수 중인 데이터"""
    )
    @DeleteMapping("/projects/{projectId}/augmented-datasets/{datasetId}")
    public CommonResponse<Void> deleteAugmentedDataset(
            @Parameter(description = "프로젝트 번호", example = "1") @PathVariable Long projectId,
            @Parameter(description = "증강 데이터셋 목록의 datasetId (= 증강 작업 번호)", example = "15") @PathVariable Long datasetId,
            @AuthenticationPrincipal CustomOAuth2User principal
    ) {
        aiResultCommandService.deleteAugmentedDataset(principal.getUser().getUserId(), projectId, datasetId);
        return CommonResponse.success(null, "증강 데이터 삭제 성공");
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
