package com.nodatanolife.bifusion_be.domain.user.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class WebsiteUpdateRequest {
    @Size(max = 255, message = "웹사이트 주소는 255자 이하로 입력해 주세요.")
    private String websiteUrl;
}
