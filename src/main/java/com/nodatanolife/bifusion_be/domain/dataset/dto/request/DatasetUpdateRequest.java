package com.nodatanolife.bifusion_be.domain.dataset.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @Size(max = 100, message = "제목은 100자 이하로 입력해 주세요.")
    private String title;

    // 데이터셋 설명 (필수)
    @NotBlank(message = "설명은 비어있을 수 없습니다.")
    @Size(max = 2000, message = "설명은 2000자 이하로 입력해 주세요.")
    private String description;

    // 아래는 전부 선택 입력값이며, 값이 없으면 null/빈 값으로 덮어써진다
    @Size(max = 50, message = "카테고리는 50자 이하로 입력해 주세요.")
    private String category;
    @Size(max = 50, message = "라이선스는 50자 이하로 입력해 주세요.")
    private String license;
    @Size(max = 20, message = "포맷은 20자 이하로 입력해 주세요.")
    private String format;
    @Size(max = 50, message = "이미지 타입은 50자 이하로 입력해 주세요.")
    private String imageType;
    @Size(max = 50, message = "해상도는 50자 이하로 입력해 주세요.")
    private String resolution;
    @Size(max = 2000, message = "클래스 정보는 2000자 이하로 입력해 주세요.")
    private String classes;
    @Size(max = 2000, message = "사용 예시는 2000자 이하로 입력해 주세요.")
    private String usageExample;
    private List<@Size(max = 50, message = "태그는 50자 이하로 입력해 주세요.") String> tags;

    // 새 파일로 교체할 때만 포함. 생략하면 기존 첨부 파일을 그대로 유지한다.
    private Long fileId;
}
