package com.nodatanolife.bifusion_be.domain.user.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class NicknameUpdateRequest {
    @Size(max = 255, message = "닉네임은 255자 이하로 입력해 주세요.")
    private String nickname;

}

