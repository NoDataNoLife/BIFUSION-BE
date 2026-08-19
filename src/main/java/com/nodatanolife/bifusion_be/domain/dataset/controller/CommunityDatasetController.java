package com.nodatanolife.bifusion_be.domain.dataset.controller;

import com.nodatanolife.bifusion_be.domain.dataset.dto.response.DatasetDetailResponse;
import com.nodatanolife.bifusion_be.domain.dataset.dto.request.DatasetCreateRequest;
import com.nodatanolife.bifusion_be.domain.dataset.dto.response.DatasetCreateResponse;
import com.nodatanolife.bifusion_be.domain.dataset.service.DatasetService;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.nodatanolife.bifusion_be.domain.dataset.dto.response.DatasetListResponse;
import com.nodatanolife.bifusion_be.global.dto.response.PageResponse;


@Tag(name = "Community Dataset", description = "커뮤니티 데이터셋 api")
@RestController
@RequestMapping("/api/v1/community/datasets")
@RequiredArgsConstructor
public class CommunityDatasetController {

    private final DatasetService datasetService;

    @Operation(
            summary = "커뮤니티 데이터셋 목록 조회",
            description = "커뮤니티에 공개(isPublic=true)된 데이터셋만 조회합니다. "
                    + "sort(LATEST: 최신순 / POPULAR: 좋아요순 / MOST_DOWNLOADED: 다운로드순), "
                    + "keyword(제목 부분 검색, 생략 가능), page, size 파라미터로 조회합니다."
    )
    @GetMapping
    public CommonResponse<PageResponse<DatasetListResponse>> getDatasets(
            @RequestParam(defaultValue = "LATEST") String sort,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<DatasetListResponse> response = PageResponse.from(
                datasetService.getDatasets(sort, keyword, page, size)
        );
        return CommonResponse.success(response, "데이터셋 목록 조회 성공!");
    }

    @Operation(
            summary = "데이터셋 상세 단건 조회",
            description = "데이터셋 상세 정보와 첨부 파일 정보를 함께 반환합니다. "
                    + "isPublic=false인 비공개 데이터셋은 작성자 본인만 조회할 수 있으며, "
                    + "본인이 아닌 사용자가 요청하면 접근이 거부됩니다."
    )
    @GetMapping("/{datasetId}")
    public CommonResponse<DatasetDetailResponse> getDatasetDetail(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long datasetId
    ) {
        Long userId = principal.getUser().getUserId();
        DatasetDetailResponse response = datasetService.getDatasetDetail(datasetId, userId);
        return CommonResponse.success(response, "데이터셋 상세 조회 성공!");
    }

    @Operation(
            summary = "데이터셋 업로드",
            description = "파일 업로드 API로 미리 받은 fileId와 함께 데이터셋 정보를 등록합니다. "
                    + "본인이 업로드한 파일이 아니면 등록에 실패합니다. "
                    + "isPublic(true: 커뮤니티 공개 / false: 내 자산 전용 비공개) 값으로 공개 범위를 지정하며, "
                    + "생략하면 기존 커뮤니티 API와 동일하게 공개(true)로 등록됩니다. "
                    + "tags를 함께 보내면 존재하지 않는 태그명은 새로 생성되어 매핑됩니다."
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<DatasetCreateResponse> createDataset(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestBody @Valid DatasetCreateRequest request
    ) {
        User user = principal.getUser();
        DatasetCreateResponse response = datasetService.createDataset(user, request);
        return CommonResponse.success(response, "데이터셋 업로드 성공!");
    }

    @Operation(
            summary = "데이터셋 삭제",
            description = "본인이 등록한 데이터셋을 삭제(soft delete)합니다. "
                    + "작성자 본인이 아닌 경우 삭제가 거부되며, 삭제된 데이터셋은 deletedAt이 기록되어 목록/상세 조회에서 제외됩니다."
    )
    @DeleteMapping("/{datasetId}")
    public CommonResponse<Void> deleteDataset(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long datasetId
    ) {
        Long userId = principal.getUser().getUserId();
        datasetService.deleteDataset(userId, datasetId);
        return CommonResponse.success(null, "데이터셋 삭제 성공!");
    }

}