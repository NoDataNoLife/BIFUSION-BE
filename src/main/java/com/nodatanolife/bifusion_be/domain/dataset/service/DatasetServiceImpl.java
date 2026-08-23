package com.nodatanolife.bifusion_be.domain.dataset.service;

import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionSummary;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;
import com.nodatanolife.bifusion_be.domain.expert.service.InspectionResultService;
import com.nodatanolife.bifusion_be.domain.tag.entity.Tag;
import com.nodatanolife.bifusion_be.domain.tag.entity.TagMapping;
import com.nodatanolife.bifusion_be.domain.tag.repository.TagRepository;
import com.nodatanolife.bifusion_be.domain.tag.repository.TagMappingRepository;
import com.nodatanolife.bifusion_be.domain.dataset.dto.response.DatasetDetailResponse;
import com.nodatanolife.bifusion_be.domain.dataset.dto.request.DatasetCreateRequest;
import com.nodatanolife.bifusion_be.domain.dataset.dto.request.DatasetUpdateRequest;
import com.nodatanolife.bifusion_be.domain.dataset.dto.response.DatasetCreateResponse;
import com.nodatanolife.bifusion_be.domain.dataset.dto.response.DatasetListResponse;
import com.nodatanolife.bifusion_be.domain.dataset.dto.response.DatasetUpdateResponse;
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
    private final InspectionResultService inspectionResultService;


    /**
     * 데이터셋 생성.
     * 1) 첨부할 파일이 요청자 본인 소유인지 검증
     * 2) 임시 파일을 영구 보관 대상으로 전환
     * 3) Dataset 엔티티 생성/저장 (isPublic은 request 값을 그대로 반영)
     * 4) 태그가 있으면 태그 마스터를 조회/생성하고 매핑을 저장
     */
    @Override
    public DatasetCreateResponse createDataset(User user, DatasetCreateRequest request) {
        File file = fileRepository.findById(request.getFileId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        // 타인이 업로드한 파일 ID를 도용해 데이터셋을 생성하지 못하도록 소유자 검증
        if (!file.getUser().getUserId().equals(user.getUserId())) {
            throw new BusinessException(ErrorCode.FILE_READ_FAILED);
        }
        // 업로드 직후 임시 상태였던 파일을 데이터셋에 실제로 연결되었으므로 영구 보관으로 전환
        file.markAsPermanent();
        Dataset dataset = Dataset.create(user, request, file);
        Dataset saved = datasetRepository.save(dataset);

        // 태그는 존재하면 재사용하고 없으면 새로 생성한 뒤, 데이터셋-태그 매핑을 저장
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


    /**
     * 커뮤니티 데이터셋 목록 조회.
     * sort 값에 따라 정렬 기준을 결정하고, 태그/전문가 검증 요약 정보를 배치로 조회해 N+1을 방지한다.
     * 실제 공개 필터링(isPublic = true)은 DatasetRepository#findAllByCondition에서 처리한다.
     */
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
        // 빈 문자열/공백 키워드는 검색 조건 없음(null)으로 취급
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword;
        Page<Dataset> datasetPage = datasetRepository.findAllByCondition(kw, pageable);
        List<Long> ids = datasetPage.getContent()
                .stream()
                .map(Dataset::getDatasetId)
                .toList();
        // 현재 페이지에 포함된 데이터셋들의 태그를 한 번에 조회해 Map으로 그룹핑 (건별 조회 방지)
        Map<Long, List<String>> tagMap = tagMappingRepository
                .findAllByTargetTypeAndTargetIdIn("DATASET", ids)
                .stream()
                .collect(Collectors.groupingBy(
                        TagMapping::getTargetId,
                        Collectors.mapping(tm -> tm.getTag().getName(), Collectors.toList())
                ));

        // 전문가 검증 요약도 동일하게 배치 조회
        Map<Long, InspectionSummary> verificationMap =
                inspectionResultService.getSummaries(InspectionTargetType.UPLOADED_DATA, ids);

        return datasetPage.map(dataset -> DatasetListResponse.from(
                dataset,
                tagMap.getOrDefault(dataset.getDatasetId(), List.of()),
                verificationMap.getOrDefault(dataset.getDatasetId(), InspectionSummary.none())
        ));
    }

    /**
     * 내 자산 데이터셋 목록 조회.
     * 공개 여부와 무관하게 본인이 작성한 데이터셋을 type(UPLOADED/AUGMENTED)으로 필터링해 반환한다.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<MyDatasetListResponse> getMyDatasets(DatasetType type, Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Dataset> datasetPage = datasetRepository.findAllByTypeAndUser_UserId(type, userId, pageable);

        List<Long> ids = datasetPage.getContent()
                .stream()
                .map(Dataset::getDatasetId)
                .toList();
        // 데이터셋 종류에 따라 검증 대상 타입도 다르게 매핑 (업로드 데이터 vs 증강 데이터)
        InspectionTargetType targetType = type == DatasetType.AUGMENTED
                ? InspectionTargetType.AUGMENTED_DATA
                : InspectionTargetType.UPLOADED_DATA;
        Map<Long, InspectionSummary> verificationMap =
                inspectionResultService.getSummaries(targetType, ids);

        return datasetPage.map(dataset -> MyDatasetListResponse.from(
                dataset,
                verificationMap.getOrDefault(dataset.getDatasetId(), InspectionSummary.none())
        ));
    }

    /**
     * 데이터셋 상세 조회.
     * 비공개 데이터셋인데 요청자가 작성자 본인이 아니면 접근을 차단한다.
     * (커뮤니티/내 자산 상세 화면이 동일 API를 공유하므로 이 가드가 유일한 비공개 방어선이다)
     */
    @Override
    @Transactional(readOnly = true)
    public DatasetDetailResponse getDatasetDetail(Long datasetId, Long userId) {
        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!dataset.getIsPublic() && !dataset.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.POST_ACCESS_DENIED);
        }

        List<String> tags = tagMappingRepository
                .findAllByTargetTypeAndTargetIdIn("DATASET", List.of(datasetId))
                .stream()
                .map(tm -> tm.getTag().getName())
                .toList();

        InspectionSummary verificationSummary =
                inspectionResultService.getSummary(InspectionTargetType.UPLOADED_DATA, datasetId);

        return DatasetDetailResponse.from(dataset, tags, verificationSummary);
    }

    /**
     * 데이터셋 삭제(soft delete). 이미 삭제된 건은 조회 대상에서 제외하고,
     * 작성자 본인이 아니면 삭제를 거부한다.
     */
    @Override
    public void deleteDataset(Long userId, Long datasetId) {
        Dataset dataset = datasetRepository.findById(datasetId)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!dataset.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.POST_ACCESS_DENIED);
        }

        dataset.delete();
    }

    /**
     * 데이터셋 수정. 작성자 본인만 수정할 수 있고, soft delete된 데이터셋은 조회 대상에서 제외해 404로 처리한다.
     * 프론트가 기존 값을 불러와 채운 폼 전체를 다시 제출하는 방식이라 title/description을 포함한
     * 대부분의 필드는 전달받은 값 그대로 덮어쓴다. fileId가 있을 때만 첨부 파일을 교체하고,
     * 태그는 항상 요청 값으로 전체 재구성한다.
     */
    @Override
    public DatasetUpdateResponse updateDataset(Long userId, Long datasetId, DatasetUpdateRequest request) {
        Dataset dataset = datasetRepository.findById(datasetId)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.DATASET_NOT_FOUND));

        if (!dataset.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.POST_ACCESS_DENIED);
        }

        if (request.getFileId() != null) {
            File file = fileRepository.findById(request.getFileId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
            if (!file.getUser().getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.FILE_READ_FAILED);
            }
            file.markAsPermanent();
            dataset.updateFile(file);
        }

        dataset.update(
                request.getTitle(),
                request.getDescription(),
                request.getCategory(),
                request.getLicense(),
                request.getFormat(),
                request.getImageType(),
                request.getResolution(),
                request.getClasses(),
                request.getUsageExample()
        );

        tagMappingRepository.deleteAllByTargetTypeAndTargetId("DATASET", datasetId);
        List<String> tagNames = request.getTags() != null ? request.getTags() : List.of();
        for (String tagName : tagNames) {
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(Tag.create(tagName)));
            tagMappingRepository.save(TagMapping.create(tag, datasetId, "DATASET"));
        }

        return DatasetUpdateResponse.from(dataset, tagNames);
    }
}