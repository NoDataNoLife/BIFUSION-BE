package com.nodatanolife.bifusion_be.domain.expert.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpertApplicationRejectRequest {

    @NotBlank(message = "반려 사유는 필수입니다.")
    @Size(max = 255, message = "반려 사유는 255자 이하로 입력해 주세요.")
    private String reason;
}
