package com.nodatanolife.bifusion_be.domain.dataset.service;

import com.nodatanolife.bifusion_be.domain.expert.dto.response.VerificationSummary;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;
import com.nodatanolife.bifusion_be.domain.expert.service.VerificationResultService;
import com.nodatanolife.bifusion_be.domain.tag.entity.Tag;
import com.nodatanolife.bifusion_be.domain.tag.entity.TagMapping;
import com.nodatanolife.bifusion_be.domain.tag.repository.TagRepository;
import com.nodatanolife.bifusion_be.domain.tag.repository.TagMappingRepository;
import com.nodatanolife.bifusion_be.domain.dataset.dto.response.DatasetDetailResponse;
import com.nodatanolife.bifusion_be.domain.dataset.dto.request.DatasetCreateRequest;
import com.nodatanolife.bifusion_be.domain.dataset.dto.response.DatasetCreateResponse;
import com.nodatanolife.bifusion_be.domain.dataset.dto.response.DatasetListResponse;
import com.nodatanolife.bifusion_be.domain.dataset.dto.response.MyDatasetListResponse;
import com.nodatanolife.bifusion_be.domain.dataset.entity.Dataset;
import com.nodatanolife.bifusion_be.domain.dataset.entity.DatasetType;
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
import java.util.Map;
import java.util.stream.Collectors;




@Service
@RequiredArgsConstructor
@Transactional
public class DatasetServiceImpl implements DatasetService {

    private final DatasetRepository datasetRepository;
    private final FileRepository fileRepository;
    private final TagRepository tagRepository;
    private final TagMappingRepository tagMappingRepository;
    private final VerificationResultService verificationResultService;


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
            case "LATEST" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "POPULAR" -> Sort.by(Sort.Direction.DESC, "likeCount");
            case "MOST_DOWNLOADED" -> Sort.by(Sort.Direction.DESC, "downloadCount");
            default -> throw new BusinessException(ErrorCode.INVALID_REQUEST);
        };

        Pageable pageable = PageRequest.of(page, size, sorting);
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword;
        Page<Dataset> datasetPage = datasetRepository.findAllByCondition(kw, pageable);
        List<Long> ids = datasetPage.getContent()
                .stream()
                .map(Dataset::getDatasetId)
                .toList();
        Map<Long, List<String>> tagMap = tagMappingRepository
                .findAllByTargetTypeAndTargetIdIn("DATASET", ids)
                .stream()
                .collect(Collectors.groupingBy(
                        TagMapping::getTargetId,
                        Collectors.mapping(tm -> tm.getTag().getName(), Collectors.toList())
                ));

        Map<Long, VerificationSummary> verificationMap =
                verificationResultService.getSummaries(InspectionTargetType.UPLOADED_DATA, ids);

        return datasetPage.map(dataset -> DatasetListResponse.from(
                dataset,
                tagMap.getOrDefault(dataset.getDatasetId(), List.of()),
                verificationMap.getOrDefault(dataset.getDatasetId(), VerificationSummary.none())
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MyDatasetListResponse> getMyDatasets(DatasetType type, Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Dataset> datasetPage = datasetRepository.findAllByTypeAndUser_UserId(type, userId, pageable);

        List<Long> ids = datasetPage.getContent()
                .stream()
                .map(Dataset::getDatasetId)
                .toList();
        InspectionTargetType targetType = type == DatasetType.AUGMENTED
                ? InspectionTargetType.AUGMENTED_DATA
                : InspectionTargetType.UPLOADED_DATA;
        Map<Long, VerificationSummary> verificationMap =
                verificationResultService.getSummaries(targetType, ids);

        return datasetPage.map(dataset -> MyDatasetListResponse.from(
                dataset,
                verificationMap.getOrDefault(dataset.getDatasetId(), VerificationSummary.none())
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public DatasetDetailResponse getDatasetDetail(Long datasetId) {
        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        List<String> tags = tagMappingRepository
                .findAllByTargetTypeAndTargetIdIn("DATASET", List.of(datasetId))
                .stream()
                .map(tm -> tm.getTag().getName())
                .toList();

        VerificationSummary verificationSummary =
                verificationResultService.getSummary(InspectionTargetType.UPLOADED_DATA, datasetId);

        return DatasetDetailResponse.from(dataset, tags, verificationSummary);
    }
}