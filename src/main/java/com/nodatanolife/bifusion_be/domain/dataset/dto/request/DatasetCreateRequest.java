package com.nodatanolife.bifusion_be.domain.dataset.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 데이터셋 생성 요청 DTO.
 * POST /api/v1/datasets 요청 바디로 사용된다.
 * (커뮤니티/내 자산 페이지 모두 이 API 하나를 공유하며, isPublic 값으로 공개 범위를 구분한다)
 */
@Getter
@NoArgsConstructor
public class DatasetCreateRequest {

    // 데이터셋 제목 (필수)
    @NotBlank(message = "제목은 비어있을 수 없습니다.")
    @Size(max = 100, message = "제목은 100자 이하로 입력해 주세요.")
    private String title;

    // 데이터셋 설명 (필수)
    @NotBlank(message = "설명은 비어있을 수 없습니다.")
    @Size(max = 2000, message = "설명은 2000자 이하로 입력해 주세요.")
    private String description;

    // 아래는 전부 선택 입력값
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

    // 사전에 파일 업로드 API로 받아온 파일 ID (필수)
    @NotNull(message = "파일 ID는 필수입니다.")
    private Long fileId;

    // 공개 여부. true(또는 생략) = 커뮤니티 공개, false = 내 자산 전용 비공개
    private Boolean isPublic;
}
