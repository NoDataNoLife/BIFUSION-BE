package com.nodatanolife.bifusion_be.domain.qna.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpertQnaRequest {

    @NotBlank(message = "질문 제목은 비어있을 수 없습니다.")
    private String title;

    @NotBlank(message = "질문 내용은 비어있을 수 없습니다.")
    private String content;

    private List<String> tags;
}
