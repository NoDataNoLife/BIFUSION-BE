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
            description = "로그인한 본인이 작성한 데이터셋만 조회합니다. 커뮤니티 목록과 달리 "
                    + "isPublic 값(공개/비공개)과 무관하게 본인 소유 데이터셋이 전부 노출됩니다. "
                    + "type(UPLOADED: 직접 업로드한 원본 데이터 / AUGMENTED: 증강 파이프라인으로 생성된 파생 데이터), "
                    + "page, size 파라미터로 조회합니다. 생성/상세/삭제는 이 컨트롤러가 아니라 "
                    + "커뮤니티 데이터셋 API(POST·GET·DELETE /api/v1/community/datasets)를 그대로 사용합니다."
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
