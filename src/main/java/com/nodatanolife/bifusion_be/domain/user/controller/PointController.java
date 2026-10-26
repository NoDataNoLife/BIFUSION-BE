package com.nodatanolife.bifusion_be.domain.user.controller;

import com.nodatanolife.bifusion_be.domain.user.dto.request.PointChargeRequest;
import com.nodatanolife.bifusion_be.domain.user.dto.response.PointChargeResponse;
import com.nodatanolife.bifusion_be.domain.user.service.PointService;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Point", description = "앤트(포인트) API")
@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @Operation(
            summary = "앤트 충전",
            description = """
                    선택한 상품만큼 앤트를 충전합니다. 결제 연동 전 체험용으로, 결제 없이 바로 지급됩니다 (임시 단가 1앤트 = 10원).

                    - points: 충전할 앤트 수. 충전 상품(500 / 1000 / 3000 / 5000)만 가능 (아니면 400, U006)
                    - 충전 기능이 꺼져 있으면 503 (U007)
                    - 충전 내역은 리워드 내역에 CHARGE로 남습니다.
                    """
    )
    @PostMapping("/charge")
    public CommonResponse<PointChargeResponse> charge(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestBody @Valid PointChargeRequest request
    ) {
        int balance = pointService.charge(principal.getUser().getUserId(), request.points());
        return CommonResponse.success(new PointChargeResponse(request.points(), balance), "앤트 충전 성공!");
    }
}
