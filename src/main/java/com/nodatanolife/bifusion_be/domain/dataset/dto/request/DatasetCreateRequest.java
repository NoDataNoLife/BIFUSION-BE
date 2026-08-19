package com.nodatanolife.bifusion_be.domain.dataset.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 데이터셋 생성 요청 DTO.
 * POST /api/v1/community/datasets 요청 바디로 사용된다.
 * (커뮤니티/내 자산 페이지 모두 이 API 하나를 공유하며, isPublic 값으로 공개 범위를 구분한다)
 */
@Getter
@NoArgsConstructor
public class DatasetCreateRequest {

    // 데이터셋 제목 (필수)
    @NotBlank(message = "제목은 비어있을 수 없습니다.")
    private String title;

    // 데이터셋 설명 (필수)
    @NotBlank(message = "설명은 비어있을 수 없습니다.")
    private String description;

    // 아래는 전부 선택 입력값
    private String category;
    private String license;
    private String format;
    private String imageType;
    private String resolution;
    private String classes;
    private String usageExample;
    private List<String> tags;

    // 사전에 파일 업로드 API로 받아온 파일 ID (필수)
    @NotNull(message = "파일 ID는 필수입니다.")
    private Long fileId;

    // 공개 여부. true(또는 생략) = 커뮤니티 공개, false = 내 자산 전용 비공개
    private Boolean isPublic;
}
