package com.nodatanolife.bifusion_be.domain.recruitment.dto.request;

import com.nodatanolife.bifusion_be.domain.recruitment.entity.RecruitApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitApplicationStatusRequest {

    @NotNull(message = "상태값은 비어있을 수 없습니다.")
    private RecruitApplicationStatus status;
}