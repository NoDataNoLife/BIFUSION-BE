package com.nodatanolife.bifusion_be.domain.comment.service;

import com.nodatanolife.bifusion_be.domain.comment.repository.CommentRepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;

    /**
     * 회원 탈퇴 시 댓글 익명화
     * Comment의 작성자를 탈퇴 계정으로 변경
     */
    @Override
    public void anonymizeByUser(User user, User deletedUser) {
        commentRepository.findAllByUser(user)
                .forEach(c -> c.anonymize(deletedUser));
    }
}
