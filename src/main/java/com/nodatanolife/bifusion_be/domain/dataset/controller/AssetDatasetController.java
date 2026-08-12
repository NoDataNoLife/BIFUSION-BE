package com.nodatanolife.bifusion_be.domain.dataset.controller;

import com.nodatanolife.bifusion_be.domain.dataset.dto.response.MyDatasetListResponse;
import com.nodatanolife.bifusion_be.domain.dataset.entity.DatasetType;
import com.nodatanolife.bifusion_be.domain.dataset.service.DatasetService;
import com.nodatanolife.bifusion_be.global.dto.response.PageResponse;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Assets Dataset", description = "에셋 데이터셋 API")
@RestController
@RequestMapping("/api/v1/assets/datasets")
@RequiredArgsConstructor
public class AssetDatasetController {

    private final DatasetService datasetService;

    @Operation(
            summary = "내 데이터셋 목록 조회",
            description = "type(UPLOADED: 업로드 데이터 / AUGMENTED: 증강 데이터), page, size 파라미터로 조회합니다."
    )
    @GetMapping
    public CommonResponse<PageResponse<MyDatasetListResponse>> getMyDatasetList(
            @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @Parameter(description = "조회 타입", schema = @Schema(allowableValues = {"UPLOADED", "AUGMENTED"}))
            @RequestParam String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = oAuth2User.getUser().getUserId();
        DatasetType listType = DatasetType.from(type);

        PageResponse<MyDatasetListResponse> response =
                PageResponse.from(datasetService.getMyDatasets(listType, userId, page, size));

        return CommonResponse.success(response, "데이터셋 목록 조회 성공!");
    }
}
