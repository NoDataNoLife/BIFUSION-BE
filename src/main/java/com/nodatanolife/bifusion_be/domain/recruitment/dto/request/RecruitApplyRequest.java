package com.nodatanolife.bifusion_be.domain.recruitment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitApplyRequest {

    @NotBlank(message = "이름은 비어있을 수 없습니다.")
    @Size(max = 255, message = "이름은 255자 이하로 입력해 주세요.")
    private String fullName;

    @NotBlank(message = "이메일은 비어있을 수 없습니다.")
    @Size(max = 255, message = "이메일은 255자 이하로 입력해 주세요.")
    private String email;

    @Size(max = 255, message = "이력서 URL은 255자 이하로 입력해 주세요.")
    private String resumeUrl; // 선택값

    @NotBlank(message = "지원 동기는 비어있을 수 없습니다.")
    @Size(max = 2000, message = "지원 동기는 2000자 이하로 입력해 주세요.")
    private String motivation;
}