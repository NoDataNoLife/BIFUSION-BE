package com.nodatanolife.bifusion_be.domain.qna.controller;

import com.nodatanolife.bifusion_be.domain.qna.dto.request.ExpertQnaRequest;
import com.nodatanolife.bifusion_be.domain.qna.dto.response.ExpertQnaResponse;
import com.nodatanolife.bifusion_be.domain.qna.service.ExpertQnAService;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/community/qna")
@Tag(name = "Expert QnA", description = "전문가 QnA API")
public class ExpertQnAController {

    private final ExpertQnAService expertQnAService;

    @PostMapping
    @Operation(summary = "전문가 QnA 질문 등록", description = "전문가 QnA에 질문을 등록합니다.")
    public CommonResponse<ExpertQnaResponse> createQna(
            @AuthenticationPrincipal CustomOAuth2User userDetails,
            @RequestBody @Valid ExpertQnaRequest request
    ) {
        ExpertQnaResponse response = expertQnAService.createQna(
                userDetails.getUser().getUserId(),
                request
        );
        return CommonResponse.success(response, "질문 등록 성공!");
    }
}
