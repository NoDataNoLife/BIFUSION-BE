package com.nodatanolife.bifusion_be.domain.job.controller;

import com.nodatanolife.bifusion_be.domain.job.dto.request.JobCreateRequest;
import com.nodatanolife.bifusion_be.domain.job.dto.response.JobResponse;
import com.nodatanolife.bifusion_be.domain.job.service.JobLaunchFacade;
import com.nodatanolife.bifusion_be.domain.job.service.JobQueryService;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI Job", description = "증강·학습·추론 작업 실행 및 상태 조회 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AiJobController {

    private final JobLaunchFacade jobLaunchFacade;
    private final JobQueryService jobQueryService;

    @Operation(
            summary = "AI 작업 실행",
            description = "type(AUGMENTATION/TRAINING/INFERENCE)에 따라 작업을 생성하고 ML 서버에 실행을 요청합니다. "
                    + "Idempotency-Key 헤더를 보내면 같은 키의 재요청은 새 작업을 만들지 않고 기존 작업을 반환합니다."
    )
    @PostMapping("/projects/{projectId}/jobs")
    public CommonResponse<JobResponse> createJob(
            @PathVariable Long projectId,
            @Valid @RequestBody JobCreateRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal CustomOAuth2User principal
    ) {
        JobResponse response = jobLaunchFacade.launch(
                principal.getUser().getUserId(), projectId, request, idempotencyKey);
        return CommonResponse.success(response, "작업 요청이 접수되었습니다.");
    }

    @Operation(summary = "AI 작업 상태 조회", description = "작업 상태와 결과를 조회합니다. 결과는 SUCCESS 상태일 때만 포함됩니다.")
    @GetMapping("/jobs/{jobId}")
    public CommonResponse<JobResponse> getJob(
            @PathVariable Long jobId,
            @AuthenticationPrincipal CustomOAuth2User principal
    ) {
        return CommonResponse.success(jobQueryService.getJob(principal.getUser().getUserId(), jobId));
    }
}
