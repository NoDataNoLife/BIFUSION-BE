package com.nodatanolife.bifusion_be.domain.qna.service;

import com.nodatanolife.bifusion_be.domain.user.entity.User;

public interface ExpertQnAService {

    void anonymizeByUser(User user, User deletedUser);
}
