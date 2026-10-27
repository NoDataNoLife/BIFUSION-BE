package com.nodatanolife.bifusion_be.domain.expert.controller;

import com.nodatanolife.bifusion_be.domain.expert.dto.response.ExpertTaskStartResponse;
import com.nodatanolife.bifusion_be.domain.expert.service.InspectionService;
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

    private final InspectionService inspectionService;

    @Operation(
            summary = "검수 시작",
            description = """
                    전문가 검수 목록(GET /api/v1/experts/me/tasks)의 검수 전(PENDING) 항목을 시작해 맡습니다.
                    검수 전 항목은 전문가 전원에게 보이는 풀이며, 먼저 시작한 전문가 한 명만 맡을 수 있습니다.

                    - id: 검수 전 항목의 inspectionRequestId (reviewCode REQ-045의 숫자와 같음)
                    - 성공하면 바로 검수 중(IN_PROGRESS)이 되고, 응답의 taskId로 상세·승인·반려 API(/api/v1/inspections/{inspectionId})를 호출합니다.
                      시작한 항목은 검수 중 그룹에 reviewCode REV-{taskId}로 나옵니다.
                    - 신청자에게 RECIPE_REVIEW_ACCEPTED 알림이 갑니다.
                    - 전문가 인증(APPROVED)이 아니면 403(EX004), 본인이 신청한 검수면 403(IS014)
                    - 신청이 없거나 취소됐으면 404(IS009), 이미 승인·반려됐으면 400(IS006)
                    - 다른 전문가가 먼저 시작했으면 409(IS015)
                    """
    )
    @PatchMapping("/{id}/start")
    public CommonResponse<ExpertTaskStartResponse> startTask(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable("id") Long inspectionRequestId
    ) {
        ExpertTaskStartResponse response = inspectionService.startInspection(principal.getUser(), inspectionRequestId);
        return CommonResponse.success(response, "검수가 시작되었습니다.");
    }
}
