package com.nodatanolife.bifusion_be.domain.user.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class LocationUpdateRequest {
    @Size(max = 255, message = "지역은 255자 이하로 입력해 주세요.")
    private String location;
}