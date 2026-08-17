package com.nodatanolife.bifusion_be.domain.expert.controller;

import com.nodatanolife.bifusion_be.domain.expert.dto.request.InspectionCreateRequest;
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
}
