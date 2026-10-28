package com.nodatanolife.bifusion_be.domain.user.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class BioUpdateRequest {
    @Size(max = 2000, message = "소개는 2000자 이하로 입력해 주세요.")
    private String bio;
}