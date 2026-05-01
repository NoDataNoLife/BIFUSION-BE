package com.nodatanolife.bifusion_be.domain.comment.service;

import com.nodatanolife.bifusion_be.domain.comment.dto.request.CommentCreateRequest;
import com.nodatanolife.bifusion_be.domain.comment.dto.response.CommentCreateResponse;
import com.nodatanolife.bifusion_be.domain.user.entity.User;

public interface CommentService {

    CommentCreateResponse createJobComment(Long projectId, Long jobId, CommentCreateRequest request, String email);

    void anonymizeByUser(User user, User deletedUser);
}
