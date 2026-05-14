package com.nodatanolife.bifusion_be.domain.job.service;

import com.nodatanolife.bifusion_be.domain.job.dto.response.JobCommentResponse;
import com.nodatanolife.bifusion_be.domain.job.entity.Job;
import com.nodatanolife.bifusion_be.domain.job.entity.JobComment;
import com.nodatanolife.bifusion_be.domain.job.repository.JobCommentRepository;
import com.nodatanolife.bifusion_be.domain.job.repository.JobRepository;
import com.nodatanolife.bifusion_be.domain.project.repository.ProjectMemberRepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class JobCommentServiceImpl implements JobCommentService {

    private final JobCommentRepository jobCommentRepository;
    private final JobRepository jobRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Override
    public JobCommentResponse createJobComment(User user, Long projectId, Long jobId, String content) {
        boolean isMember = projectMemberRepository.existsByProjectProjectIdAndUserUserId(projectId, user.getUserId());
        if (!isMember) {
            throw new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED);
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        JobComment jobComment = JobComment.create(user, job, content);
        JobComment savedComment = jobCommentRepository.save(jobComment);

        return JobCommentResponse.from(savedComment);
    }

    @Override
    public void anonymizeByUser(User user, User deletedUser) {
        List<Job> jobs = jobRepository.findAllByUser(user);
        jobs.forEach(job -> job.updateUser(deletedUser));

        List<JobComment> comments = jobCommentRepository.findAllByUser(user);
        comments.forEach(comment -> comment.updateUser(deletedUser));
    }

    @Override
    public void deleteJobComment(User user, Long projectId, Long commentId) {
        boolean isMember = projectMemberRepository.existsByProjectProjectIdAndUserUserId(projectId, user.getUserId());
        if (!isMember) {
            throw new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED);
        }

        JobComment comment = jobCommentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!comment.getUser().getUserId().equals(user.getUserId())) {
            throw new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED);
        }

        jobCommentRepository.delete(comment);
    }
}