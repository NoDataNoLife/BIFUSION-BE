package com.nodatanolife.bifusion_be.domain.recruitment.controller;

import com.nodatanolife.bifusion_be.domain.recruitment.dto.request.RecruitApplicationStatusRequest;
import com.nodatanolife.bifusion_be.domain.recruitment.dto.response.RecruitApplicationStatusResponse;
import com.nodatanolife.bifusion_be.domain.recruitment.dto.response.RecruitmentListResponse;
import com.nodatanolife.bifusion_be.domain.recruitment.dto.request.RecruitmentRequest;
import com.nodatanolife.bifusion_be.domain.recruitment.dto.response.RecruitmentResponse;
import com.nodatanolife.bifusion_be.domain.recruitment.service.RecruitService;
import com.nodatanolife.bifusion_be.domain.recruitment.dto.request.RecruitApplyRequest;
import com.nodatanolife.bifusion_be.domain.recruitment.dto.response.RecruitApplyResponse;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.dto.response.PageResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Recruitment", description = "팀원 모집 API")
@RestController
@RequestMapping("/api/v1/community/recruitments")
@RequiredArgsConstructor
public class RecruitmentController {

    private final RecruitService recruitService;

    @PostMapping
    @Operation(summary = "팀원 모집 공고 등록", description = "새로운 팀원 모집 공고를 등록합니다.")
    public CommonResponse<RecruitmentResponse> createRecruitment(
            @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @RequestBody @Valid RecruitmentRequest request
    ) {
        RecruitmentResponse response = recruitService.createRecruitment(request, oAuth2User.getUser());
        return CommonResponse.success(response, "팀원 모집 공고가 등록되었습니다!");
    }

    @Operation(summary = "팀원 지원하기", description = "팀원 모집글에 지원합니다.")
    @PostMapping("/{recruitmentId}/apply")
    public CommonResponse<RecruitApplyResponse> applyToRecruitment(
            @PathVariable Long recruitmentId,
            @RequestBody @Valid RecruitApplyRequest request,
            @AuthenticationPrincipal CustomOAuth2User oAuth2User
    ) {
        Long userId = oAuth2User.getUser().getUserId();

        RecruitApplyResponse response = recruitService.applyToRecruitment(recruitmentId, request, userId);

        return CommonResponse.success(response, "팀원 지원이 완료되었습니다!");
    }

    @Operation(summary = "팀원 지원 승인/거절", description = "모집글 작성자가 지원자의 상태를 ACCEPTED 또는 REJECTED로 변경합니다.")
    @PatchMapping("/{recruitmentId}/applications/{applicationId}/status")
    public CommonResponse<RecruitApplicationStatusResponse> updateApplicationStatus(
            @PathVariable Long recruitmentId,
            @PathVariable Long applicationId,
            @RequestBody @Valid RecruitApplicationStatusRequest request,
            @AuthenticationPrincipal CustomOAuth2User oAuth2User
    ) {
        Long userId = oAuth2User.getUser().getUserId();

        RecruitApplicationStatusResponse response =
                recruitService.updateApplicationStatus(recruitmentId, applicationId, request, userId);

        return CommonResponse.success(response, "지원 상태 변경이 완료되었습니다!");
    }

    @Operation(summary = "팀원 모집 목록 조회", description = "sort(LATEST/DEADLINE_SOON), page, size 파라미터로 조회합니다.")
    @GetMapping
    public CommonResponse<PageResponse<RecruitmentListResponse>> getRecruitmentList(
            @RequestParam(defaultValue = "LATEST") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<RecruitmentListResponse> response = PageResponse.from(
                recruitService.getRecruitmentList(sort, page, size)
        );

        return CommonResponse.success(response, "팀원 모집 목록 조회 성공!");
    }
}
