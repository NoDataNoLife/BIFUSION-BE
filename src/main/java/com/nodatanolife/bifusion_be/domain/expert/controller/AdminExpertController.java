package com.nodatanolife.bifusion_be.domain.expert.controller;

import com.nodatanolife.bifusion_be.domain.expert.dto.request.ExpertApplicationRejectRequest;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.AdminExpertApplicationResponse;
import com.nodatanolife.bifusion_be.domain.expert.entity.ExpertApplicationStatus;
import com.nodatanolife.bifusion_be.domain.expert.service.ExpertService;
import com.nodatanolife.bifusion_be.global.dto.response.PageResponse;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 전용 전문가 인증 심사 API. /api/v1/admin/** 는 SecurityConfig에서 ROLE_ADMIN만 허용한다.
 */
@Tag(name = "Admin - Expert", description = "관리자 전문가 인증 심사 API")
@RestController
@RequestMapping("/api/v1/admin/expert-applications")
@RequiredArgsConstructor
public class AdminExpertController {

    private final ExpertService expertService;

    @Operation(
            summary = "전문가 인증 신청 목록 조회 (관리자)",
            description = """
                    전문가 인증 신청 목록을 신청 시각 오래된 순으로 페이지 조회합니다.

                    - status: PENDING/APPROVED/REJECTED (생략하면 전체)
                    - documentUrl은 인증 서류의 presigned URL입니다.
                    - 관리자(role = ADMIN)가 아니면 403이 반환됩니다.
                    """
    )
    @GetMapping
    public CommonResponse<PageResponse<AdminExpertApplicationResponse>> getApplications(
            @RequestParam(required = false) ExpertApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "appliedAt"));
        Page<AdminExpertApplicationResponse> result = expertService.getApplications(status, pageable);
        return CommonResponse.success(PageResponse.from(result), "전문가 인증 신청 목록 조회 성공!");
    }

    @Operation(
            summary = "전문가 인증 승인 (관리자)",
            description = """
                    PENDING 상태의 신청을 승인하고 신청자에게 EXPERT_APPROVED 알림을 보냅니다.

                    - 신청이 없으면 404(EX008), PENDING이 아니면 409(EX007)가 반환됩니다.
                    """
    )
    @PatchMapping("/{applicationId}/approve")
    public CommonResponse<Void> approve(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long applicationId
    ) {
        expertService.approve(principal.getUser().getUserId(), applicationId);
        return CommonResponse.success(null, "전문가 인증 승인 성공!");
    }

    @Operation(
            summary = "전문가 인증 반려 (관리자)",
            description = """
                    PENDING 상태의 신청을 반려하고, 반려 사유를 담아 신청자에게 EXPERT_REJECTED 알림을 보냅니다.

                    - reason: 반려 사유 (필수, 최대 255자)
                    - 신청이 없으면 404(EX008), PENDING이 아니면 409(EX007)가 반환됩니다.
                    """
    )
    @PatchMapping("/{applicationId}/reject")
    public CommonResponse<Void> reject(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long applicationId,
            @RequestBody @Valid ExpertApplicationRejectRequest request
    ) {
        expertService.reject(principal.getUser().getUserId(), applicationId, request.getReason());
        return CommonResponse.success(null, "전문가 인증 반려 성공!");
    }
}
