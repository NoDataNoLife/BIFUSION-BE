package com.nodatanolife.bifusion_be.domain.expert.dto.request;

import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InspectionCreateRequest {

    @NotNull(message = "검수 대상 종류는 필수입니다.")
    private InspectionTargetType targetType;

    @NotNull(message = "대상 ID는 필수입니다.")
    @Positive(message = "대상 ID는 양수여야 합니다.")
    private Long targetId;

    @NotBlank(message = "검증 요청 메모는 필수입니다.")
    private String reason;

    @NotNull(message = "리워드 포인트는 필수입니다.")
    @Positive(message = "리워드 포인트는 양수여야 합니다.")
    private Integer rewardPoints;
}
