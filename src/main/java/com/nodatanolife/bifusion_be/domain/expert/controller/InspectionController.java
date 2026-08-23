package com.nodatanolife.bifusion_be.domain.expert.controller;

import com.nodatanolife.bifusion_be.domain.expert.dto.request.DraftCommentRequest;
import com.nodatanolife.bifusion_be.domain.expert.dto.request.ImageCommentRequest;
import com.nodatanolife.bifusion_be.domain.expert.dto.request.InspectionCreateRequest;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionDetailResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionDraftResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionImageCommentResponse;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionResponse;
import com.nodatanolife.bifusion_be.domain.expert.service.InspectionService;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Inspection", description = "전문가 검수 신청 API")
@RestController
@RequestMapping("/api/v1/inspections")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;

    @Operation(
            summary = "전문가 검수 신청",
            description = """
                    내 레시피, 업로드 데이터, 증강 데이터에 대해 전문가 검수를 신청합니다.

                    - targetType: 검수 대상 종류 (RECIPE / UPLOADED_DATA / AUGMENTED_DATA)
                    - targetId: 검수를 신청할 대상의 고유 ID
                    - reason: 전문가가 중점적으로 봐야 할 사항을 적는 검증 요청 메모 (필수)
                    - rewardPoints: 전문가에게 제공할 리워드 포인트 (필수, 신청과 동시에 즉시 차감)
                    - 신청자 본인이 소유한 대상에 한해 신청할 수 있습니다. (본인 소유가 아니면 403)
                    - 존재하지 않는 대상이면 404가 반환됩니다.
                    - 같은 사용자가 같은 대상에 이미 신청한 이력이 있으면 409가 반환됩니다.
                    - 보유 포인트가 rewardPoints보다 부족하면 400이 반환됩니다.
                    - 신청이 성공하면 rewardPoints만큼 포인트가 차감되고, 대상(레시피/업로드 데이터)의 검증 상태가 PENDING으로 전환되며, 상태값 PENDING으로 검수 요청이 생성됩니다.
                    """
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<InspectionResponse> createInspection(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestBody @Valid InspectionCreateRequest request
    ) {
        User user = principal.getUser();
        InspectionResponse response = inspectionService.createInspection(user, request);
        return CommonResponse.success(response, "전문가 검수 신청이 완료되었습니다.");
    }

    @Operation(
            summary = "검수 중인 job 상세 조회",
            description = """
                    전문가가 검수 대기/진행 중인 특정 검수 요청 하나를 상세로 조회합니다.

                    - inspectionId: 검수 시작 API(PATCH /api/v1/experts/me/tasks/{taskId}/start)와 동일한 리소스 ID입니다.
                    - 전문가 인증(APPROVED)을 받지 않은 사용자가 요청하면 403이 반환됩니다.
                    - 존재하지 않거나 본인에게 배정되지 않은 검수 요청이면 404가 반환됩니다.
                    - expertComment는 이전에 임시 저장한 코멘트이며, 없으면 null입니다.
                    - sampleImages.images의 각 항목은 imageUrl뿐 아니라 label(분류 라벨)과 comment(이미지별 검수 의견)도 함께 내려줍니다.
                      "이미지 정밀 검수" 모달을 열 때 별도 API 호출 없이 바로 채울 수 있도록 상세 조회 시점에 전부 포함합니다.
                    """
    )
    @GetMapping("/{inspectionId}")
    public CommonResponse<InspectionDetailResponse> getInspectionDetail(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long inspectionId
    ) {
        InspectionDetailResponse response = inspectionService.getInspectionDetail(principal.getUser(), inspectionId);
        return CommonResponse.success(response, "검수 상세 조회 성공");
    }

    @Operation(
            summary = "검수 최종 코멘트 임시 저장",
            description = """
                    검수 상세 화면 상단의 "임시 저장" 버튼에서 호출합니다.

                    - 승인/거절 등 검수 상태(status)는 변경하지 않고 expertComment만 저장합니다.
                    - 임시 저장이므로 빈 문자열도 그대로 허용합니다.
                    - 전문가 인증(APPROVED)을 받지 않은 사용자가 요청하면 403이 반환됩니다.
                    - 존재하지 않거나 본인에게 배정되지 않은 검수 요청이면 404가 반환됩니다.
                    """
    )
    @PatchMapping("/{inspectionId}/draft")
    public CommonResponse<InspectionDraftResponse> saveDraft(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long inspectionId,
            @RequestBody @Valid DraftCommentRequest request
    ) {
        InspectionDraftResponse response = inspectionService.saveDraftComment(
                principal.getUser(), inspectionId, request.getExpertComment());
        return CommonResponse.success(response, "임시 저장 완료");
    }

    @Operation(
            summary = "이미지별 검수 의견 저장",
            description = """
                    "이미지 정밀 검수" 모달의 "코멘트 저장" 버튼에서 호출합니다.

                    - 임시 저장이므로 빈 문자열도 그대로 허용합니다.
                    - 전문가 인증(APPROVED)을 받지 않은 사용자가 요청하면 403이 반환됩니다.
                    - 존재하지 않거나 본인에게 배정되지 않은 검수 요청이면 404가 반환됩니다.
                    - imageId가 해당 검수 건의 샘플 이미지가 아니면 404가 반환됩니다.
                    """
    )
    @PatchMapping("/{inspectionId}/images/{imageId}/comment")
    public CommonResponse<InspectionImageCommentResponse> saveImageComment(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long inspectionId,
            @PathVariable Long imageId,
            @RequestBody @Valid ImageCommentRequest request
    ) {
        InspectionImageCommentResponse response = inspectionService.saveImageComment(
                principal.getUser(), inspectionId, imageId, request.getComment());
        return CommonResponse.success(response, "이미지 코멘트 저장 완료");
    }
}
