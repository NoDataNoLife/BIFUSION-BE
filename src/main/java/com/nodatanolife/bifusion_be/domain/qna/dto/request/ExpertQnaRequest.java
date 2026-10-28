package com.nodatanolife.bifusion_be.domain.qna.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpertQnaRequest {

    @NotBlank(message = "질문 제목은 비어있을 수 없습니다.")
    @Size(max = 100, message = "질문 제목은 100자 이하로 입력해 주세요.")
    private String title;

    @NotBlank(message = "질문 내용은 비어있을 수 없습니다.")
    @Size(max = 2000, message = "질문 내용은 2000자 이하로 입력해 주세요.")
    private String content;

    private List<@Size(max = 50, message = "태그는 50자 이하로 입력해 주세요.") String> tags;
}
