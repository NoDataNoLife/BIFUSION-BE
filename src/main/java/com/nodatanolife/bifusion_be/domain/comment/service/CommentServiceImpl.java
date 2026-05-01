package com.nodatanolife.bifusion_be.domain.comment.service;

import com.nodatanolife.bifusion_be.domain.comment.dto.request.CommentCreateRequest;
import com.nodatanolife.bifusion_be.domain.comment.dto.response.CommentCreateResponse;
import com.nodatanolife.bifusion_be.domain.comment.entity.Comment;
import com.nodatanolife.bifusion_be.domain.comment.repository.CommentRepository;
import com.nodatanolife.bifusion_be.domain.job.entity.Job;
import com.nodatanolife.bifusion_be.domain.job.repository.JobRepository;
import com.nodatanolife.bifusion_be.domain.project.repository.ProjectMemberRepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;

    /**
     * 회원 탈퇴 시 댓글 익명화
     * Comment의 작성자를 탈퇴 계정으로 변경
     */
    @Override
    public void anonymizeByUser(User user, User deletedUser) {
        commentRepository.findAllByUser(user)
                .forEach(c -> c.anonymize(deletedUser));
    }

    /**
     * 팀 코멘트 작성
     */
    @Override
    @Transactional
    public CommentCreateResponse createJobComment(Long projectId, Long jobId, CommentCreateRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        boolean isMember = projectMemberRepository
                .existsByProjectProjectIdAndUserUserId(projectId, user.getUserId());
        if (!isMember) {
            throw new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED);
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        Comment comment = Comment.create(user, job.getJobId(), "JOB", request.content());
        commentRepository.save(comment);

        return CommentCreateResponse.of(comment);
    }

}
