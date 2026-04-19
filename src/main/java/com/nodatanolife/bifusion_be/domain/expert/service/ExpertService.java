package com.nodatanolife.bifusion_be.domain.expert.service;

import com.nodatanolife.bifusion_be.domain.user.entity.User;

public interface ExpertService {

    void deleteApplicationByUser(User user);

    void anonymizeByUser(User user, User deletedUser);
}
