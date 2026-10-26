package com.nodatanolife.bifusion_be.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record PointChargeResponse(
        @Schema(description = "충전한 앤트 수", example = "500")
        int chargedPoints,
        @Schema(description = "충전 후 보유 앤트", example = "60000")
        int balance
) {
}
