package com.nodatanolife.bifusion_be.domain.job.controller;

import com.nodatanolife.bifusion_be.domain.job.dto.request.JobCommentRequest;
import com.nodatanolife.bifusion_be.domain.job.dto.response.JobCommentResponse;
import com.nodatanolife.bifusion_be.domain.job.service.JobCommentService;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@Tag(name = "Comment", description = "Comment api")
@RestController
@RequestMapping("/api/v1/projects/{projectId}/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobCommentService jobCommentService;

    @Operation(summary = "Job comment 작성", description = "특정 Job에 댓글을 작성합니다.")
    @PostMapping("/{jobId}/comments")
    public CommonResponse<JobCommentResponse> createComment(
            @PathVariable Long projectId,
            @PathVariable Long jobId,
            @Valid @RequestBody JobCommentRequest request,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        User user = customOAuth2User.getUser();

        JobCommentResponse response = jobCommentService.createJobComment(user, projectId, jobId, request.getContent());

        return CommonResponse.success(response, "댓글이 성공적으로 등록되었습니다.");
    }

    @Operation(summary = "Job comment 수정", description = "특정 Job의 댓글을 수정합니다.")
    @PutMapping("/{jobId}/comments/{commentId}")
    public CommonResponse<JobCommentResponse> updateComment(
            @PathVariable Long projectId,
            @PathVariable Long jobId,
            @PathVariable Long commentId,
            @Valid @RequestBody JobCommentRequest request,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        User user = customOAuth2User.getUser();
        JobCommentResponse response = jobCommentService.updateJobComment(user, commentId, request.getContent());
        return CommonResponse.success(response, "댓글이 성공적으로 수정되었습니다.");
    }
}

