package com.nodatanolife.bifusion_be.domain.comment.controller;

import com.nodatanolife.bifusion_be.domain.comment.dto.request.CommentUpdateRequest;
import com.nodatanolife.bifusion_be.domain.comment.dto.response.CommentUpdateResponse;
import com.nodatanolife.bifusion_be.domain.comment.service.CommentService;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects")
public class CommentController {

    private final CommentService commentService;

    /**
     * 팀 코멘트 수정 (PUT)
     */
    @PutMapping("/{projectId}/jobs/{jobId}/comments/{commentId}")
    public CommonResponse<CommentUpdateResponse> updateJobComment(
            @PathVariable Long projectId,
            @PathVariable Long jobId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request,
            @AuthenticationPrincipal CustomOAuth2User oAuth2User) {

        String email = oAuth2User.getUser().getEmail();

        CommentUpdateResponse response = commentService.updateJobComment(commentId, request, email);

        return CommonResponse.success(response,"댓글이 성공적으로 수정되었습니다");    }
}