package com.nodatanolife.bifusion_be.domain.job.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobCommentRequest {

    @NotBlank(message = "댓글 내용은 비어있을 수 없습니다.")
    private String content;
}