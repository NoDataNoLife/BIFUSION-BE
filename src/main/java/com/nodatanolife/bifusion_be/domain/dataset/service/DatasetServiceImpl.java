package com.nodatanolife.bifusion_be.domain.dataset.service;

import com.nodatanolife.bifusion_be.domain.tag.entity.Tag;
import com.nodatanolife.bifusion_be.domain.tag.entity.TagMapping;
import com.nodatanolife.bifusion_be.domain.tag.repository.TagRepository;
import com.nodatanolife.bifusion_be.domain.tag.repository.TagMappingRepository;
import com.nodatanolife.bifusion_be.domain.dataset.dto.request.DatasetCreateRequest;
import com.nodatanolife.bifusion_be.domain.dataset.dto.response.DatasetCreateResponse;
import com.nodatanolife.bifusion_be.domain.dataset.dto.response.DatasetListResponse;
import com.nodatanolife.bifusion_be.domain.dataset.entity.Dataset;
import com.nodatanolife.bifusion_be.domain.dataset.repository.DatasetRepository;
import com.nodatanolife.bifusion_be.domain.file.entity.File;
import com.nodatanolife.bifusion_be.domain.file.repository.FileRepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;


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

        Dataset dataset = Dataset.create(user, request, file);
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

    @Override
    @Transactional(readOnly = true)
    public Page<DatasetListResponse> getDatasets(String sort, String keyword, int page, int size) {
        Sort sorting = switch (sort) {
            case "POPULAR"         -> Sort.by(Sort.Direction.DESC, "likeCount");
            case "MOST_DOWNLOADED" -> Sort.by(Sort.Direction.DESC, "downloadCount");
            default                -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        Pageable pageable = PageRequest.of(page, size, sorting);
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword;

        return datasetRepository.findAllByCondition(kw, pageable)
                .map(dataset -> {
                    List<String> tags = tagMappingRepository
                            .findAllByTargetTypeAndTargetId("DATASET", dataset.getDatasetId())
                            .stream()
                            .map(tm -> tm.getTag().getName())
                            .toList();
                    return DatasetListResponse.from(dataset, tags);
                });
    }
}

