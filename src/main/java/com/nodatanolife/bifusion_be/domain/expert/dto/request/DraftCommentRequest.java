package com.nodatanolife.bifusion_be.domain.expert.dto.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DraftCommentRequest {

    private String expertComment;
}
