package com.nodatanolife.bifusion_be.domain.expert.controller;

import com.nodatanolife.bifusion_be.domain.expert.dto.response.ExpertTaskStartResponse;
import com.nodatanolife.bifusion_be.domain.expert.service.ExpertTaskService;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ExpertTask", description = "전문가 검수 작업 수행 API")
@RestController
@RequestMapping("/api/v1/experts/me/tasks")
@RequiredArgsConstructor
public class ExpertTaskController {

    private final ExpertTaskService expertTaskService;

    @Operation(
            summary = "검수 시작",
            description = "검수전(PENDING) 상태의 검수 요청을 검수중(IN_PROGRESS)으로 전환합니다."
    )
    @PatchMapping("/{taskId}/start")
    public CommonResponse<ExpertTaskStartResponse> startTask(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long taskId
    ) {
        ExpertTaskStartResponse response = expertTaskService.startTask(principal.getUser(), taskId);
        return CommonResponse.success(response, "검수가 시작되었습니다.");
    }
}
