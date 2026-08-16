package com.nodatanolife.bifusion_be.domain.dataset.service;

import com.nodatanolife.bifusion_be.domain.dataset.dto.response.DatasetDetailResponse;
import com.nodatanolife.bifusion_be.domain.dataset.dto.request.DatasetCreateRequest;
import com.nodatanolife.bifusion_be.domain.dataset.dto.response.DatasetCreateResponse;
import com.nodatanolife.bifusion_be.domain.dataset.dto.response.DatasetListResponse;
import com.nodatanolife.bifusion_be.domain.dataset.dto.response.MyDatasetListResponse;
import com.nodatanolife.bifusion_be.domain.dataset.entity.DatasetType;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.domain.Page;


public interface DatasetService {

    DatasetCreateResponse createDataset(User user, DatasetCreateRequest request);

    void anonymizeByUser(User user, User deletedUser);

    Page<DatasetListResponse> getDatasets(String sort, String keyword, int page, int size);

    Page<MyDatasetListResponse> getMyDatasets(DatasetType type, Long userId, int page, int size);

    DatasetDetailResponse getDatasetDetail(Long datasetId);

    void deleteDataset(Long userId, Long datasetId);
}