package com.nodatanolife.bifusion_be.domain.qna.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpertQnAAnswerRequest {

    @NotBlank(message = "답변 내용은 비어있을 수 없습니다.")
    private String content;
}