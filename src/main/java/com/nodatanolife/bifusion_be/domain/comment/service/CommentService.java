package com.nodatanolife.bifusion_be.domain.comment.service;

import com.nodatanolife.bifusion_be.domain.user.entity.User;

public interface CommentService {

    void anonymizeByUser(User user, User deletedUser);
}
