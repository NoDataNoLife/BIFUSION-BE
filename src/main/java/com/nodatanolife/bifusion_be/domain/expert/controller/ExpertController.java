package com.nodatanolife.bifusion_be.domain.expert.controller;

import com.nodatanolife.bifusion_be.domain.expert.dto.response.ExpertApplicationResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.ExpertPerformanceResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.ExpertTaskGroupedResponse;
import com.nodatanolife.bifusion_be.domain.expert.service.ExpertService;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Expert", description = "전문가 인증/대시보드 API")
@RestController
@RequestMapping("/api/v1/experts")
@RequiredArgsConstructor
public class ExpertController {

    private final ExpertService expertService;

    @Operation(
            summary = "전문가 인증 신청",
            description = """
                    의료 면허증 또는 학위 증명서 파일을 업로드해 전문가 인증을 신청합니다.

                    - certificationFile: 인증 서류 파일 (필수, 확장자 pdf/jpg/jpeg/png, 최대 10MB)
                    - 이미 전문가 인증(APPROVED)을 받은 사용자가 다시 신청하면 409가 반환됩니다.
                    - PENDING 상태의 신청이 이미 있으면 기존 신청 건의 첨부 파일만 새 파일로 교체합니다. (심사 큐에는 1건만 유지)
                    - 그 외의 경우 새로운 신청이 PENDING 상태로 생성됩니다.
                    - 파일이 없거나 허용되지 않은 확장자이면 400(EXPERT_FILE_INVALID)이 반환됩니다.
                    - 파일 용량이 10MB를 초과하면 400(EXPERT_FILE_TOO_LARGE)이 반환됩니다.
                    """
    )
    @PostMapping("/me")
    public CommonResponse<ExpertApplicationResponse> applyExpert(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestPart MultipartFile certificationFile
    ) {
        ExpertApplicationResponse response = expertService.apply(
                principal.getUser().getUserId(), certificationFile);
        return CommonResponse.success(response, "전문가 인증 신청 성공!");
    }

    @Operation(
            summary = "전문가 성과 대시보드 조회",
            description = """
                    로그인한 전문가의 성과 요약을 반환합니다.
                    전문가 인증(APPROVED)을 받지 않은 사용자가 호출하면 403이 반환됩니다.

                    - isExpertCertified: 전문가 인증 완료 여부 (이 응답이 오면 항상 true)
                    - totalReward: 총 획득 리워드 포인트
                    - topPercent: 검수 완료 건수 기준 상위 퍼센타일
                    - completedCount: 검수 완료 건수
                    - feedbackSatisfaction: 피드백 만족도 평균 (5점 만점, 소수 첫째자리 반올림, 데이터 없으면 null)
                    - taskStats: 검수 상태별 건수 (PENDING/IN_PROGRESS/COMPLETED)
                    """
    )
    @GetMapping("/me/performance")
    public CommonResponse<ExpertPerformanceResponse> getMyPerformance(
            @AuthenticationPrincipal CustomOAuth2User principal
    ) {
        ExpertPerformanceResponse response = expertService.getMyPerformance(principal.getUser());
        return CommonResponse.success(response, "전문가 성과 조회 성공!");
    }

    @Operation(
            summary = "전문가 검수 요청 목록 조회",
            description = """
                    로그인한 전문가에게 배정된 검수 요청 목록을 검수 상태별로 묶어 반환합니다. (증강 데이터 검수 대상)
                    전문가 인증(APPROVED)을 받지 않은 사용자가 호출하면 403이 반환됩니다.

                    - PENDING: 검수 전 목록
                    - IN_PROGRESS: 검수 중 목록
                    - COMPLETED: 검수 후 목록
                    - 각 그룹 내 정렬 기준: updatedAt 최신순
                    - 각 항목의 reviewCode: 검수 요청 코드 (예: REV-001)
                    - 각 항목의 reviewResult: 검수 완료 건의 판정 결과 (APPROVED / REJECTED), 미완료 건은 null
                    - 각 항목의 requester: 검수를 신청한 사용자 (userId, nickname, email)
                    - 각 항목의 targetType: 검수 대상 종류 (AUGMENTED_DATA / RECIPE / UPLOADED_DATA)
                    - 각 항목의 targetTitle: 검수 대상 이름 (증강 데이터는 프로젝트명, 레시피·데이터셋은 제목)
                    - 각 항목의 targetImageUrl: 검수 대상 대표 이미지 (증강 데이터는 증강 결과 미리보기 이미지, 레시피는 썸네일, 데이터셋은 배너). 없으면 null
                    - 각 항목의 projectName: 증강 데이터가 속한 프로젝트명 (레시피·데이터셋 검수는 null)
                    - 각 항목의 requestedAt: 검수 신청일
                    - 각 항목의 updatedAt: 검수 건 최종 수정 일시
                    """
    )
    @GetMapping("/me/tasks")
    public CommonResponse<ExpertTaskGroupedResponse> getMyTasks(
            @AuthenticationPrincipal CustomOAuth2User principal
    ) {
        ExpertTaskGroupedResponse response = expertService.getMyTasks(principal.getUser());
        return CommonResponse.success(response, "검수 요청 목록 조회 성공!");
    }
}
