package com.nodatanolife.bifusion_be.domain.job.service;

import com.nodatanolife.bifusion_be.domain.job.dto.response.JobCommentResponse;
import com.nodatanolife.bifusion_be.domain.user.entity.User;

public interface JobCommentService {

    JobCommentResponse createJobComment(User user, Long projectId, Long jobId, String content);


    JobCommentResponse updateJobComment(User user, Long commentId, String content);

    void anonymizeByUser(User user, User deletedUser);
}