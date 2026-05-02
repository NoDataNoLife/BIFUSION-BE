package com.nodatanolife.bifusion_be.domain.comment.controller;

import com.nodatanolife.bifusion_be.domain.comment.service.CommentService;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @DeleteMapping("/{projectId}/jobs/{jobId}/comments/{commentId}")
    public CommonResponse<Void> deleteJobComment(
            @PathVariable Long projectId,
            @PathVariable Long jobId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomOAuth2User oAuth2User) {
        String email = oAuth2User.getUser().getEmail();
        commentService.deleteJobComment(projectId, jobId, commentId, email);
        return CommonResponse.success(null,"댓글이 성공적으로 삭제되었습니다.");    }
}