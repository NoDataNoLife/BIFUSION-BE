package com.nodatanolife.bifusion_be.domain.comment.controller;

import com.nodatanolife.bifusion_be.domain.comment.dto.request.CommentCreateRequest;
import com.nodatanolife.bifusion_be.domain.comment.dto.response.CommentCreateResponse;
import com.nodatanolife.bifusion_be.domain.comment.service.CommentService;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/{projectId}/jobs/{jobId}/comments")
    public CommonResponse<CommentCreateResponse> createJobComment(
            @PathVariable Long projectId,
            @PathVariable Long jobId,
            @Valid @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal CustomOAuth2User oAuth2User) {
        String email = oAuth2User.getUser().getEmail();
        CommentCreateResponse response = commentService.createJobComment(projectId, jobId, request, email);
        return CommonResponse.success(response, "댓글이 성공적으로 등록되었습니다.");    }
}