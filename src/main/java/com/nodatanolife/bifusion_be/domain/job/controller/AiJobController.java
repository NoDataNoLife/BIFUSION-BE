package com.nodatanolife.bifusion_be.domain.job.controller;

import com.nodatanolife.bifusion_be.domain.job.dto.request.JobCreateRequest;
import com.nodatanolife.bifusion_be.domain.job.dto.response.JobCostEstimateResponse;
import com.nodatanolife.bifusion_be.domain.job.dto.response.JobResponse;
import com.nodatanolife.bifusion_be.domain.job.dto.response.ProjectJobListResponse;
import com.nodatanolife.bifusion_be.domain.job.entity.JobStatus;
import com.nodatanolife.bifusion_be.domain.job.entity.JobType;
import com.nodatanolife.bifusion_be.domain.job.service.JobCancelFacade;
import com.nodatanolife.bifusion_be.domain.job.service.JobCostEstimateService;
import com.nodatanolife.bifusion_be.domain.job.service.JobLaunchFacade;
import com.nodatanolife.bifusion_be.domain.job.service.JobQueryService;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI Job", description = "증강·학습·추론 작업 실행, 목록·상태 조회, 재시도, 취소 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AiJobController {

    private static final String IDEMPOTENCY_KEY_DESCRIPTION =
            "중복 실행 방지 키(선택, 100자 이하). 버튼을 누를 때마다 새 값(예: UUID)을 만들고, 같은 값은 네트워크 재전송에만 사용합니다. "
                    + "같은 키로 다시 요청하면 새 작업을 만들지 않고 기존 작업을 반환합니다.";

    private final JobLaunchFacade jobLaunchFacade;
    private final JobCancelFacade jobCancelFacade;
    private final JobQueryService jobQueryService;
    private final JobCostEstimateService jobCostEstimateService;

    @Operation(
            summary = "AI 작업 실행",
            description = """
                    type(AUGMENTATION / TRAINING / INFERENCE)에 따라 작업을 만들고 ML 서버에 실행을 요청합니다. 응답은 PENDING 상태의 작업입니다.
                    - 이미지는 먼저 /api/v1/files/temp에 업로드하고 받은 fileId를 보냅니다.
                    - 학습 dataset_id는 증강 데이터셋 목록의 datasetId, 추론 model_id는 학습 모델 목록의 trainedModelId입니다.
                    - 생략한 설정값은 서버 기본값으로 채워지고, 실제 사용한 값이 기록됩니다.
                    - 진행 상태는 GET /api/v1/jobs/{jobId}로 확인합니다.
                    - 실행 비용(앤트)은 작업을 만들 때 차감되고, 실패·취소되면 전액 돌려받습니다. 금액은 POST /api/v1/jobs/cost-estimate로 미리 확인합니다.

                    주요 오류
                    - 400 J004 설정값 범위 위반 / J005 이미지 장수 위반 / J006 같은 이미지 중복 / J007 학습에 쓸 수 없는 증강 데이터 / F006 JPG·PNG 아님 / F007 이미지 합계 용량 초과 / IS004 앤트 부족(작업을 만들지 않음)
                    - 403 P002 프로젝트 멤버 아님 / F005 본인이 올린 파일 아님
                    - 404 J008 모델 없음 / F004 파일 없음
                    - 409 F008 이미 다른 작업에 쓴 이미지 / J009 다른 요청에 쓴 Idempotency-Key
                    - 503 ML001 AI 서버 연결 불가 / ML006 AI 서버 응답 지연 (작업은 실패로 기록, 자동 재시도 없음)"""
    )
    @PostMapping("/projects/{projectId}/jobs")
    public CommonResponse<JobResponse> createJob(
            @Parameter(description = "프로젝트 번호", example = "1") @PathVariable Long projectId,
            @Valid @RequestBody JobCreateRequest request,
            @Parameter(description = IDEMPOTENCY_KEY_DESCRIPTION)
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal CustomOAuth2User principal
    ) {
        JobResponse response = jobLaunchFacade.launch(
                principal.getUser().getUserId(), projectId, request, idempotencyKey);
        return CommonResponse.success(response, "작업 요청이 접수되었습니다.");
    }

    @Operation(
            summary = "AI 작업 예상 비용",
            description = """
                    실행 요청과 같은 본문을 보내면 실행 시 차감될 앤트와 현재 보유 앤트를 돌려줍니다. 작업은 만들지 않습니다.
                    - 생략한 설정값은 실행할 때와 같은 서버 기본값으로 채워 계산합니다.
                    - 비용은 설정값으로만 정해지고, 추론만 이미지 수(fileIds 장수)가 들어갑니다. 증강·학습은 이미지·데이터셋을 고르기 전에도 조회할 수 있습니다.
                    - 다시 시도는 원본과 같은 설정이라 원본 작업의 chargedPoints와 같은 금액이 차감됩니다.

                    주요 오류
                    - 400 J004 설정값 범위 위반 / J005 추론 이미지 장수 위반 / J006 같은 이미지 중복"""
    )
    @PostMapping("/jobs/cost-estimate")
    public CommonResponse<JobCostEstimateResponse> estimateJobCost(
            @RequestBody JobCreateRequest request,
            @AuthenticationPrincipal CustomOAuth2User principal
    ) {
        JobCostEstimateResponse response = jobCostEstimateService.estimate(principal.getUser().getUserId(), request);
        return CommonResponse.success(response, "작업 예상 비용 조회 성공");
    }

    @Operation(
            summary = "프로젝트 AI 작업 목록",
            description = """
                    프로젝트의 작업을 최신순(생성 시각 내림차순)으로 조회합니다. 커서 기반이며, 다음 페이지는 응답의 nextCursor를 cursor로 보냅니다.
                    - 상태는 조회 시점 값입니다. 화면 갱신은 PENDING·RUNNING 작업이 있을 때만 첫 페이지를 주기적으로 다시 조회합니다.
                    - statusCounts는 첫 페이지(cursor 없음)에서만 내려가며, type·keyword 필터는 반영하고 status 필터는 반영하지 않습니다.
                    - 실패 사유는 error(사용자용 문구)로만 내려갑니다.

                    주요 오류
                    - 400 C005 잘못된 cursor / C002 잘못된 type·status 값
                    - 403 P002 프로젝트 멤버 아님"""
    )
    @GetMapping("/projects/{projectId}/jobs")
    public CommonResponse<ProjectJobListResponse> getProjectJobs(
            @Parameter(description = "프로젝트 번호", example = "1") @PathVariable Long projectId,
            @Parameter(description = "작업 종류 필터 (생략 시 전체)") @RequestParam(required = false) JobType type,
            @Parameter(description = "작업 상태 필터 (생략 시 전체, 하나만 선택)") @RequestParam(required = false) JobStatus status,
            @Parameter(description = "검색어. JOB-015 형식이나 숫자면 작업 번호 일치, 그 외는 실행자 닉네임 부분 일치(대소문자 무시). 빈 값이면 조건 없음",
                    example = "JOB-015")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "이전 응답의 nextCursor (첫 페이지는 생략)") @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지 크기 (기본 20, 최대 50)", example = "20") @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomOAuth2User principal
    ) {
        return CommonResponse.success(jobQueryService.getProjectJobs(
                principal.getUser().getUserId(), projectId, type, status, keyword, cursor, size));
    }

    @Operation(
            summary = "AI 작업 재시도",
            description = """
                    실패·취소된 작업을 같은 설정(시드 포함)과 같은 입력 이미지로 다시 실행하고, 새 작업을 반환합니다. 원래 작업은 기록으로 남습니다.
                    - 응답의 retryable이 true인 작업만 가능합니다. 다시 시도는 가장 최근 작업에서만 할 수 있습니다.
                    - 프로젝트 멤버 누구나 할 수 있습니다. 요청 본문은 없습니다.
                    - 실행 실패 시 응답 규칙(503 등)은 작업 실행 API와 같습니다.

                    주요 오류
                    - 400 J007 원래 쓰던 증강 데이터를 더 이상 쓸 수 없음
                    - 403 P002 프로젝트 멤버 아님
                    - 404 J003 작업 없음 / J008 원래 쓰던 모델 없음
                    - 409 J010 진행 중·완료된 작업 / J011 입력 문제로 실패한 작업(새로 실행 필요) / J012 이미 다시 시도한 작업
                    - 503 ML001 AI 서버 연결 불가"""
    )
    @PostMapping("/jobs/{jobId}/retry")
    public CommonResponse<JobResponse> retryJob(
            @Parameter(description = "다시 시도할 작업 번호", example = "15") @PathVariable Long jobId,
            @Parameter(description = IDEMPOTENCY_KEY_DESCRIPTION)
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal CustomOAuth2User principal
    ) {
        JobResponse response = jobLaunchFacade.retry(principal.getUser().getUserId(), jobId, idempotencyKey);
        return CommonResponse.success(response, "작업 재시도 요청이 접수되었습니다.");
    }

    @Operation(
            summary = "AI 작업 취소",
            description = """
                    진행 중인 작업을 취소합니다. 작업을 만든 사람이나 프로젝트 관리자(MANAGER)만 할 수 있습니다.
                    - 이미 취소된 작업은 현재 상태를 그대로 반환합니다(반복 요청 허용).
                    - 취소 기능이 꺼져 있으면(ML 취소 기능 배포 전) 503 J016을 반환하고 상태는 바뀌지 않습니다.
                    - 409를 받으면 GET /api/v1/jobs/{jobId}로 최종 상태를 다시 조회합니다(409 응답에는 작업 상태가 없음).

                    주요 오류
                    - 403 P002 프로젝트 멤버 아님 / J015 본인·관리자 아님
                    - 404 J003 작업 없음
                    - 409 J001 이미 끝난 작업 / J013 작업 시작 중 / J014 AI 서버에서 이미 끝남
                    - 502 ML003 AI 서버 응답 오류
                    - 503 J016 취소 기능 비활성화 / ML001 AI 서버 연결 불가"""
    )
    @PostMapping("/jobs/{jobId}/cancel")
    public CommonResponse<JobResponse> cancelJob(
            @Parameter(description = "취소할 작업 번호", example = "15") @PathVariable Long jobId,
            @AuthenticationPrincipal CustomOAuth2User principal
    ) {
        return CommonResponse.success(
                jobCancelFacade.cancel(principal.getUser().getUserId(), jobId), "작업이 취소되었습니다.");
    }

    @Operation(
            summary = "AI 작업 상태 조회",
            description = """
                    작업 상태와 결과를 조회합니다. DB에 저장된 값만 반환하며, AI 서버 상태는 서버가 주기적으로 반영합니다.
                    - result는 SUCCESS일 때만 채워지며, 작업 종류별로 형식이 다릅니다.
                    - 진행 화면에서 이 API를 주기적으로 조회해 상태 변화를 확인합니다.

                    주요 오류
                    - 403 P002 프로젝트 멤버 아님
                    - 404 J003 작업 없음"""
    )
    @GetMapping("/jobs/{jobId}")
    public CommonResponse<JobResponse> getJob(
            @Parameter(description = "작업 번호", example = "15") @PathVariable Long jobId,
            @AuthenticationPrincipal CustomOAuth2User principal
    ) {
        return CommonResponse.success(jobQueryService.getJob(principal.getUser().getUserId(), jobId));
    }
}
