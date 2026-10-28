package com.nodatanolife.bifusion_be.domain.expert.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageCommentRequest {

    @Size(max = 2000, message = "이미지 코멘트는 2000자 이하로 입력해 주세요.")
    private String comment;
}
