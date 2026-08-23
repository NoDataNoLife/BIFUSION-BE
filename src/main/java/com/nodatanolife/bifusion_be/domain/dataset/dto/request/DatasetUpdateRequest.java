package com.nodatanolife.bifusion_be.domain.dataset.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 데이터셋 수정 요청 DTO.
 * PUT /api/v1/datasets/{datasetId}/upload 요청 바디로 사용된다.
 * 프론트가 기존 값을 불러와 채운 폼 전체를 다시 제출하는 방식이라, title/description을 포함한
 * 대부분의 필드는 부분 수정이 아닌 전달받은 값 그대로 덮어쓴다. fileId만 예외로, 생략하면 기존
 * 첨부 파일을 유지하고 값이 있을 때만 새 파일로 교체한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DatasetUpdateRequest {

    // 데이터셋 제목 (필수)
    @NotBlank(message = "제목은 비어있을 수 없습니다.")
    private String title;

    // 데이터셋 설명 (필수)
    @NotBlank(message = "설명은 비어있을 수 없습니다.")
    private String description;

    // 아래는 전부 선택 입력값이며, 값이 없으면 null/빈 값으로 덮어써진다
    private String category;
    private String license;
    private String format;
    private String imageType;
    private String resolution;
    private String classes;
    private String usageExample;
    private List<String> tags;

    // 새 파일로 교체할 때만 포함. 생략하면 기존 첨부 파일을 그대로 유지한다.
    private Long fileId;
}
