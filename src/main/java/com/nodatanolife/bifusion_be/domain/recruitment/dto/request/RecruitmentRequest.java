package com.nodatanolife.bifusion_be.domain.recruitment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentRequest {

    @NotBlank(message = "포지션 제목은 비어있을 수 없습니다.")
    @Size(max = 100, message = "포지션 제목은 100자 이하로 입력해 주세요.")
    private String jobTitle;

    @NotBlank(message = "소속/기관은 비어있을 수 없습니다.")
    @Size(max = 255, message = "소속/기관은 255자 이하로 입력해 주세요.")
    private String organization;

    @NotBlank(message = "포지션 설명은 비어있을 수 없습니다.")
    private String description;

    private List<String> requirements;

    private List<String> responsibilities;

    private List<String> benefits;

    private Integer teamSize;

    private LocalDate deadline;

    private List<@Size(max = 50, message = "태그는 50자 이하로 입력해 주세요.") String> tags;
}