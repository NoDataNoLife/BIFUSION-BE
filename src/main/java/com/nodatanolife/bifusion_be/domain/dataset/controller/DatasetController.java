package com.nodatanolife.bifusion_be.domain.dataset.controller;

import com.nodatanolife.bifusion_be.domain.dataset.dto.request.DatasetUpdateRequest;
import com.nodatanolife.bifusion_be.domain.dataset.dto.response.DatasetUpdateResponse;
import com.nodatanolife.bifusion_be.domain.dataset.service.DatasetService;
import com.nodatanolife.bifusion_be.global.response.CommonResponse;
import com.nodatanolife.bifusion_be.global.security.oauth2.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dataset", description = "데이터셋 API")
@RestController
@RequestMapping("/api/v1/datasets")
@RequiredArgsConstructor
public class DatasetController {

    private final DatasetService datasetService;

    @Operation(
            summary = "데이터셋 수정",
            description = "본인이 등록한 데이터셋의 정보를 수정합니다. 프론트가 상세 조회(GET) 응답으로 "
                    + "기존 값을 채운 폼 전체를 다시 제출하는 방식이며, title/description을 포함한 대부분의 "
                    + "필드는 전달받은 값 그대로 덮어씁니다. fileId는 새 파일로 교체할 때만 포함하고, "
                    + "생략하면 기존 첨부 파일이 유지됩니다. tags는 항상 요청 값으로 전체 재구성됩니다. "
                    + "작성자 본인이 아닌 경우 수정이 거부됩니다."
    )
    @PutMapping("/{datasetId}/upload")
    public CommonResponse<DatasetUpdateResponse> updateDataset(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long datasetId,
            @RequestBody @Valid DatasetUpdateRequest request
    ) {
        Long userId = principal.getUser().getUserId();
        DatasetUpdateResponse response = datasetService.updateDataset(userId, datasetId, request);
        return CommonResponse.success(response, "데이터셋 수정 성공!");
    }
}
