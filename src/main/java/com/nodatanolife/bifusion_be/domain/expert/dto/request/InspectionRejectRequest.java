package com.nodatanolife.bifusion_be.domain.expert.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InspectionRejectRequest {

    @NotBlank(message = "거절 사유는 필수입니다.")
    private String expertComment;
}
