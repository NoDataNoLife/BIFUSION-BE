package com.nodatanolife.bifusion_be.domain.expert.controller;

import com.nodatanolife.bifusion_be.domain.expert.dto.request.DraftCommentRequest;
import com.nodatanolife.bifusion_be.domain.expert.dto.request.ImageCommentRequest;
import com.nodatanolife.bifusion_be.domain.expert.dto.request.InspectionApproveRequest;
import com.nodatanolife.bifusion_be.domain.expert.dto.request.InspectionCreateRequest;
import com.nodatanolife.bifusion_be.domain.expert.dto.request.InspectionRejectRequest;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionDecisionResponse;
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

@Tag(name = "Inspection", description = "전문가 검수 신청/처리 API. 조회·처리 API는 전문가 인증(APPROVED) 아니면 403, 존재하지 않거나 본인에게 배정되지 않은 검수 요청이면 404.")
@RestController
@RequestMapping("/api/v1/inspections")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;

    @Operation(
            summary = "전문가 검수 신청",
            description = """
                    내 증강 데이터·레시피·업로드 데이터셋에 대해 전문가 검수를 신청합니다.

                    - targetType/targetId: 검수 대상. 각 화면 응답의 inspectionTarget 값을 그대로 보냅니다.
                      - AUGMENTED_DATA: AugmentationConfig ID
                      - RECIPE: Recipe ID. 원본을 그대로 복제한 레시피(POST /community/recipes/{recipeId}/fork)는 신청 불가(400, IS011).
                        원본 내용을 불러와 다시 등록한 레시피(등록 API + forkedFromId)는 신청 가능
                      - UPLOADED_DATA: 업로드 Dataset ID. 증강 데이터셋은 AUGMENTED_DATA로 신청
                    - reason: 검증 요청 메모(필수), rewardPoints: 전문가 리워드(신청 즉시 차감)
                    - rewardPoints는 대상별 범위 안이어야 합니다(벗어나면 400, IS013). 범위는 각 화면 응답의 inspectionTarget.minRewardPoints·maxRewardPoints
                      - 대상의 스토리지 크기(MB, 올림) 기준. 증강 데이터는 증강 결과 데이터셋 파일, 업로드 데이터셋은 첨부 파일
                      - 최소: max(100, MB × 10), 최대: min(10,000, max(1,000, MB × 100))
                      - 레시피와 크기를 알 수 없는 대상: 100 ~ 1,000
                    - 대상이 없거나 삭제됐으면 404, 본인 소유 대상만 신청 가능(아니면 403), 중복 신청 409, 포인트 부족 400
                    - 신청과 동시에 전문가 1명에게 자동 배정됩니다 (진행 중인 검수가 가장 적은 전문가, 신청자 본인 제외).
                      배정할 전문가가 없으면 409(IS008)이며 포인트는 차감되지 않습니다.
                    - 반려·취소된 대상은 다시 신청할 수 있습니다.
                    - 응답의 inspectionRequestId는 검수 신청 ID로, 취소 API에 사용합니다. 전문가용 /inspections/{inspectionId} API의 inspectionId(배정 ID)와 다릅니다.
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
            summary = "전문가 검수 신청 취소",
            description = """
                    내 검수 신청을 취소하고 차감된 포인트를 돌려받습니다. 전문가가 검수를 시작하기 전에만 취소할 수 있습니다.

                    - inspectionRequestId: 검수 신청 API 응답의 inspectionRequestId, 또는 증강 데이터셋 목록의 inspectionTarget.inspectionRequestId
                    - 취소 후 같은 대상에 다시 신청할 수 있습니다.
                    - 본인 신청이 아니거나 없으면 404(IS009), 검수가 시작됐거나 이미 처리된 신청은 409(IS010)
                    """
    )
    @PatchMapping("/requests/{inspectionRequestId}/cancel")
    public CommonResponse<InspectionResponse> cancelInspection(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long inspectionRequestId
    ) {
        InspectionResponse response = inspectionService.cancelInspection(principal.getUser(), inspectionRequestId);
        return CommonResponse.success(response, "전문가 검수 신청이 취소되었습니다.");
    }

    @Operation(
            summary = "검수 중인 job 상세 조회",
            description = """
                    검수 대기/진행 중인 요청 하나를 상세 조회합니다. inspectionId는 검수 시작 API(PATCH /api/v1/experts/me/tasks/{taskId}/start)와 동일한 리소스입니다.

                    - targetType에 해당하는 영역만 채워지고 나머지는 null입니다.
                      - AUGMENTED_DATA: jobTitle, previewUrl(증강 결과 미리보기), augmentationParams, sampleImages
                      - RECIPE: recipe (개요·생성 설정값)
                      - UPLOADED_DATA: dataset (메타데이터·첨부 파일. 파일은 fileId로 POST /api/v1/files/{fileId}/download)
                    - reason: 신청자가 남긴 검증 요청 메모
                    - expertComment: 확정 코멘트(승인/거절 시 저장된 값)가 있으면 그 값, 없으면 draft 값
                    - sampleImages.images[]에 label/comment까지 포함 ("이미지 정밀 검수" 모달 프리페치용)
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
                    상세 화면 상단 "임시 저장" 버튼. status는 바꾸지 않고 expertComment만 저장합니다(빈 문자열 허용).
                    이미 승인/거절로 확정된 건은 400.
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
                    "이미지 정밀 검수" 모달의 "코멘트 저장" 버튼. draft 개념 없이 바로 저장됩니다(빈 문자열 허용).
                    증강 데이터 검수 전용입니다. imageId가 해당 검수 건의 샘플 이미지가 아니거나 레시피·데이터셋 검수 건이면 404, 이미 승인/거절로 확정된 건은 400.
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

    @Operation(
            summary = "검수 최종 승인",
            description = """
                    하단 "최종 승인하기" 버튼. PENDING만 가능(아니면 400). expertComment는 선택값(비면 코멘트 없이 승인).
                    draft는 폐기되고 요청값이 최종 코멘트로 확정되며, 담당 전문가에게 ExpertTask.reward의 90%(수수료 10% 제외, 반올림)가 즉시 지급됩니다.
                    """
    )
    @PatchMapping("/{inspectionId}/approve")
    public CommonResponse<InspectionDecisionResponse> approveInspection(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long inspectionId,
            @RequestBody @Valid InspectionApproveRequest request
    ) {
        InspectionDecisionResponse response = inspectionService.approveInspection(
                principal.getUser(), inspectionId, request.getExpertComment());
        return CommonResponse.success(response, "검수 승인 완료");
    }

    @Operation(
            summary = "검수 최종 거절",
            description = """
                    하단 "거절하기" 버튼. PENDING만 가능(아니면 400). expertComment(거절 사유)는 필수(비면 400).
                    draft는 폐기되고 요청값이 최종 코멘트로 확정되며, 승인과 동일하게 담당 전문가에게 ExpertTask.reward의 90%가 즉시 지급됩니다.
                    """
    )
    @PatchMapping("/{inspectionId}/reject")
    public CommonResponse<InspectionDecisionResponse> rejectInspection(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long inspectionId,
            @RequestBody @Valid InspectionRejectRequest request
    ) {
        InspectionDecisionResponse response = inspectionService.rejectInspection(
                principal.getUser(), inspectionId, request.getExpertComment());
        return CommonResponse.success(response, "검수 거절 완료");
    }
}
