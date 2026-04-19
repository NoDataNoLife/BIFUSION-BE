package com.nodatanolife.bifusion_be.domain.comment.repository;

import com.nodatanolife.bifusion_be.domain.comment.entity.Comment;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findAllByUser(User user);
}
