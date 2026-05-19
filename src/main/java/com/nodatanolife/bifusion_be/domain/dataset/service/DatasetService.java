package com.nodatanolife.bifusion_be.domain.dataset.service;

import com.nodatanolife.bifusion_be.domain.dataset.dto.request.DatasetCreateRequest;
import com.nodatanolife.bifusion_be.domain.dataset.dto.response.DatasetCreateResponse;
import com.nodatanolife.bifusion_be.domain.user.entity.User;

public interface DatasetService {

    DatasetCreateResponse createDataset(User user, DatasetCreateRequest request);

    void anonymizeByUser(User user, User deletedUser);
}