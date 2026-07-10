package com.nodatanolife.bifusion_be.domain.recruitment.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitApplyRequest {

    @NotBlank(message = "이름은 비어있을 수 없습니다.")
    private String fullName;

    @NotBlank(message = "이메일은 비어있을 수 없습니다.")
    private String email;

    private String resumeUrl; // 선택값

    @NotBlank(message = "지원 동기는 비어있을 수 없습니다.")
    private String motivation;
}