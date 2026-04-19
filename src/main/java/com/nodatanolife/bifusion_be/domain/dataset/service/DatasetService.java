package com.nodatanolife.bifusion_be.domain.dataset.service;

import com.nodatanolife.bifusion_be.domain.user.entity.User;

public interface DatasetService {

    void anonymizeByUser(User user, User deletedUser);
}
