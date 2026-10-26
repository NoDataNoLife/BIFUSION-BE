package com.nodatanolife.bifusion_be.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record PointChargeRequest(
        @Schema(description = "충전할 앤트 수 (충전 화면 상품: 500 / 1000 / 3000 / 5000)", example = "500")
        @NotNull(message = "충전할 앤트 수는 필수입니다.")
        Integer points
) {
}
