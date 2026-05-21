package com.nodatanolife.bifusion_be.domain.dataset.controller;

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


@Tag(name = "Dataset", description = "Dataset api")
@RestController
@RequestMapping("/api/v1/community/datasets")
@RequiredArgsConstructor
public class DatasetController {

    private final DatasetService datasetService;

    @Operation(summary = "데이터셋 업로드", description = "파일 업로드 API로 받은 fileId와 함께 데이터셋 정보를 등록합니다.")
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


    @Operation(summary = "데이터셋 목록 조회", description = "sort(LATEST/POPULAR/MOST_DOWNLOADED), keyword, page, size 파라미터로 조회합니다.")
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