package com.nodatanolife.bifusion_be.domain.expert.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InspectionRejectRequest {

    @NotBlank(message = "거절 사유는 필수입니다.")
    @Size(max = 2000, message = "거절 사유는 2000자 이하로 입력해 주세요.")
    private String expertComment;
}
