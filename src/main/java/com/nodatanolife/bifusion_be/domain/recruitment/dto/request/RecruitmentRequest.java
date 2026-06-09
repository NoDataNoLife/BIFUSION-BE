package com.nodatanolife.bifusion_be.domain.recruitment.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentRequest {

    @NotBlank(message = "포지션 제목은 비어있을 수 없습니다.")
    private String jobTitle;

    @NotBlank(message = "소속/기관은 비어있을 수 없습니다.")
    private String organization;

    @NotBlank(message = "포지션 설명은 비어있을 수 없습니다.")
    private String description;

    private List<String> requirements;

    private List<String> responsibilities;

    private List<String> benefits;

    private Integer teamSize;

    private LocalDate deadline;

    private List<String> tags;
}