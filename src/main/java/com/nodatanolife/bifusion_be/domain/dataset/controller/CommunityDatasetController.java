package com.nodatanolife.bifusion_be.domain.dataset.controller;

import com.nodatanolife.bifusion_be.domain.dataset.dto.response.DatasetListResponse;
import com.nodatanolife.bifusion_be.domain.dataset.service.DatasetService;
import com.nodatanolife.bifusion_be.global.dto.response.PageResponse;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
                    + "keyword(제목 부분 검색, 생략 가능), page, size 파라미터로 조회합니다. "
                    + "상세 조회/생성/수정/삭제는 이 컨트롤러가 아니라 단건 리소스 API(/api/v1/datasets)를 사용합니다."
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
}
