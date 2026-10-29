package com.nodatanolife.bifusion_be.domain.dataset.service;

import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionSummary;
import com.nodatanolife.bifusion_be.domain.expert.dto.response.InspectionTargetResponse;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;
import com.nodatanolife.bifusion_be.domain.expert.service.InspectionResultService;
import com.nodatanolife.bifusion_be.domain.expert.service.InspectionService;
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
import com.nodatanolife.bifusion_be.domain.file.service.S3ObjectCleaner;
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
import java.util.ArrayList;
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
    private final InspectionService inspectionService;
    private final DatasetBannerExtractor datasetBannerExtractor;
    private final S3ObjectCleaner s3ObjectCleaner;


    /**
     * 데이터셋 생성.
     * 1) 첨부할 파일이 요청자 본인 소유인지 검증
     * 2) 임시 파일을 영구 보관 대상으로 전환
     * 3) Dataset 엔티티 생성/저장 (isPublic은 request 값을 그대로 반영)
     * 4) 첨부 파일에서 대표 이미지를 뽑아 저장 (뽑지 못하면 비워 둔다)
     * 5) 태그가 있으면 태그 마스터를 조회/생성하고 매핑을 저장
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
        dataset.updateBannerImage(datasetBannerExtractor.extract(file));
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
        // Dataset 행은 항상 업로드 데이터라(증강 데이터셋은 AI 결과 목록 API가 AugmentationConfig 기준으로 보여준다)
        // 검수 상태도 UPLOADED_DATA 기준으로만 조회한다. type=AUGMENTED면 목록이 비어 있다.
        Map<Long, InspectionSummary> verificationMap =
                inspectionResultService.getSummaries(InspectionTargetType.UPLOADED_DATA, ids);
        Map<Long, InspectionTargetResponse> targetMap =
                inspectionResultService.getUploadedDataTargets(userId, datasetPage.getContent());

        return datasetPage.map(dataset -> MyDatasetListResponse.from(
                dataset,
                verificationMap.getOrDefault(dataset.getDatasetId(), InspectionSummary.none()),
                targetMap.get(dataset.getDatasetId())
        ));
    }

    /**
     * 데이터셋 상세 조회.
     * 삭제된 데이터셋은 수정·삭제 API와 같이 404로 처리한다 (작성자 본인 포함).
     * 비공개 데이터셋인데 요청자가 작성자 본인이 아니면 접근을 차단한다.
     * (커뮤니티/내 자산 상세 화면이 동일 API를 공유하므로 이 가드가 유일한 비공개 방어선이다)
     */
    @Override
    @Transactional(readOnly = true)
    public DatasetDetailResponse getDatasetDetail(Long datasetId, Long userId) {
        Dataset dataset = datasetRepository.findById(datasetId)
                .filter(d -> !d.isDeleted())
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
        // 검수 신청 정보(신청 ID 포함)는 작성자 본인에게만 내려준다. 증강 데이터셋은 결과 맵에 없어 null
        InspectionTargetResponse inspectionTarget = dataset.getUser().getUserId().equals(userId)
                ? inspectionResultService.getUploadedDataTargets(userId, List.of(dataset)).get(datasetId)
                : null;

        return DatasetDetailResponse.from(dataset, tags, verificationSummary, inspectionTarget);
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

        // 전문가가 시작하기 전인 검수 신청은 자동 취소·환불하고, 이미 시작됐으면 삭제를 막는다(409)
        if (dataset.getType() == DatasetType.UPLOADED) {
            inspectionService.cancelForTargetDeletion(userId, InspectionTargetType.UPLOADED_DATA, datasetId);
        }
        dataset.delete();
        deleteStoredObjects(dataset);
    }

    /**
     * 삭제한 데이터셋의 첨부 파일·대표 이미지를 S3에서 지운다 (커밋 후). 스토리지 사용량은 실제 객체 기준이라 함께 줄어든다.
     * File 행은 삭제된 데이터셋이 계속 가리키므로 남긴다 (사용량 집계에서는 빠지고, 다운로드는 404).
     * 같은 파일을 쓰는 다른 데이터셋이 남아 있으면 파일은 지우지 않는다.
     */
    private void deleteStoredObjects(Dataset dataset) {
        File file = dataset.getFile();
        List<String> keys = new ArrayList<>();
        boolean fileInUse = file != null
                && datasetRepository.existsByFileAndDatasetIdNotAndDeletedAtIsNull(file, dataset.getDatasetId());
        if (file != null && !fileInUse) {
            keys.add(file.getS3Key());
        }
        String banner = dataset.getBannerImageUrl();
        // 첨부 파일이 이미지면 대표 이미지가 첨부 파일 자체라, 파일을 지울 때만 함께 지워진다
        if (banner != null && (file == null || !banner.equals(file.getS3Key()))) {
            keys.add(banner);
        }
        s3ObjectCleaner.deleteAfterCommit(keys);
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
            File previousFile = dataset.getFile();
            String previousBanner = dataset.getBannerImageUrl();
            file.markAsPermanent();
            dataset.updateFile(file);
            // 대표 이미지는 첨부 파일에서 뽑으므로 파일이 바뀌면 다시 뽑는다
            dataset.updateBannerImage(datasetBannerExtractor.extract(file));
            deleteReplacedObjects(dataset, previousFile, previousBanner);
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

    /**
     * 교체돼 더 이상 쓰지 않는 이전 첨부 파일(File 행 + S3 객체)과 이전 대표 이미지를 지운다.
     * 이전 파일을 다른 데이터셋(삭제된 것 포함)이 가리키고 있으면 남긴다.
     */
    private void deleteReplacedObjects(Dataset dataset, File previousFile, String previousBanner) {
        List<String> keys = new ArrayList<>();
        if (previousFile != null && !previousFile.getFileId().equals(dataset.getFile().getFileId())
                && !datasetRepository.existsByFileAndDatasetIdNot(previousFile, dataset.getDatasetId())) {
            fileRepository.delete(previousFile);
            keys.add(previousFile.getS3Key());
        }
        // 이전 대표 이미지가 이전 첨부 파일 자체였으면 위에서 파일과 함께 처리된다
        boolean bannerIsPreviousFile = previousFile != null && previousFile.getS3Key().equals(previousBanner);
        if (previousBanner != null && !bannerIsPreviousFile && !previousBanner.equals(dataset.getBannerImageUrl())) {
            keys.add(previousBanner);
        }
        s3ObjectCleaner.deleteAfterCommit(keys);
    }

    @Override
    public boolean backfillBannerImage(Long datasetId) {
        Dataset dataset = datasetRepository.findById(datasetId).orElse(null);
        if (dataset == null || dataset.getBannerImageUrl() != null) {
            return false;
        }
        String bannerImage = datasetBannerExtractor.extract(dataset.getFile());
        dataset.updateBannerImage(bannerImage);
        return bannerImage != null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dataset> getPublicDatasetsByUser(Long userId) {
        return datasetRepository.findPublicByUserId(userId);
    }
}