package com.nodatanolife.bifusion_be.domain.recruitment.service;

import com.nodatanolife.bifusion_be.domain.user.entity.User;

public interface RecruitService {

    void anonymizeByUser(User user, User deletedUser);
}
