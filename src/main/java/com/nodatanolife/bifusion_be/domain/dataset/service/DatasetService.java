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

    /**
     * 데이터셋을 생성한다. request.isPublic()에 따라 공개(커뮤니티 노출) 또는
     * 비공개(내 자산 전용)로 저장된다.
     */
    DatasetCreateResponse createDataset(User user, DatasetCreateRequest request);

    /**
     * 회원 탈퇴 시 해당 유저가 작성한 모든 데이터셋의 작성자를 탈퇴 계정으로 치환한다.
     */
    void anonymizeByUser(User user, User deletedUser);

    /**
     * 커뮤니티 데이터셋 목록을 조회한다. 공개(isPublic = true) 데이터셋만 대상이다.
     */
    Page<DatasetListResponse> getDatasets(String sort, String keyword, int page, int size);

    /**
     * 내 자산 데이터셋 목록을 조회한다. 공개 여부와 무관하게 본인 소유 데이터셋을 type으로 필터링한다.
     */
    Page<MyDatasetListResponse> getMyDatasets(DatasetType type, Long userId, int page, int size);

    /**
     * 데이터셋 상세를 조회한다. 비공개 데이터셋이면 작성자 본인(userId)만 조회할 수 있다.
     */
    DatasetDetailResponse getDatasetDetail(Long datasetId, Long userId);

    /**
     * 데이터셋을 soft delete한다. 작성자 본인만 삭제할 수 있다.
     */
    void deleteDataset(Long userId, Long datasetId);
}