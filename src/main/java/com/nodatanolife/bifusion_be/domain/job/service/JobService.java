package com.nodatanolife.bifusion_be.domain.job.service;

import com.nodatanolife.bifusion_be.domain.user.entity.User;

public interface JobService {

    void anonymizeByUser(User user, User deletedUser);
}
