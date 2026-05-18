package com.nodatanolife.bifusion_be.domain.dataset.service;

import com.nodatanolife.bifusion_be.domain.tag.Tag;
import com.nodatanolife.bifusion_be.domain.tag.TagMapping;
import com.nodatanolife.bifusion_be.domain.tag.repository.TagRepository;
import com.nodatanolife.bifusion_be.domain.tag.repository.TagMappingRepository;
import com.nodatanolife.bifusion_be.domain.dataset.dto.request.DatasetCreateRequest;
import com.nodatanolife.bifusion_be.domain.dataset.dto.response.DatasetCreateResponse;
import com.nodatanolife.bifusion_be.domain.dataset.entity.Dataset;
import com.nodatanolife.bifusion_be.domain.dataset.repository.DatasetRepository;
import com.nodatanolife.bifusion_be.domain.file.entity.File;
import com.nodatanolife.bifusion_be.domain.file.repository.FileRepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DatasetServiceImpl implements DatasetService {

    private final DatasetRepository datasetRepository;
    private final FileRepository fileRepository;
    private final TagRepository tagRepository;
    private final TagMappingRepository tagMappingRepository;

    @Override
    public DatasetCreateResponse createDataset(User user, DatasetCreateRequest request) {
        File file = fileRepository.findById(request.getFileId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!file.getUser().getUserId().equals(user.getUserId())) {
            throw new BusinessException(ErrorCode.FILE_READ_FAILED);
        }

        file.markAsPermanent();

        Dataset dataset = Dataset.create(user, request, file.getFileUrl());
        Dataset saved = datasetRepository.save(dataset);

        if (request.getTags() != null && !request.getTags().isEmpty()) {
            for (String tagName : request.getTags()) {
                Tag tag = tagRepository.findByName(tagName)
                        .orElseGet(() -> tagRepository.save(Tag.create(tagName)));
                tagMappingRepository.save(TagMapping.create(tag, saved.getDatasetId(), "DATASET"));
            }
        }

        return DatasetCreateResponse.from(saved);
    }

    /**
     * 회원 탈퇴 시 데이터셋 익명화
     * Dataset의 owner를 탈퇴 계정으로 변경
     */
    @Override
    public void anonymizeByUser(User user, User deletedUser) {
        datasetRepository.findAllByUser(user)
                .forEach(d -> d.anonymize(deletedUser));
    }
}

