package com.nodatanolife.bifusion_be.domain.dataset.dto.request;

import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
public class DatasetCreateRequest {

    @NotBlank(message = "제목은 비어있을 수 없습니다.")
    private String title;

    @NotBlank(message = "설명은 비어있을 수 없습니다.")
    private String description;

    private String category;
    private String license;
    private String format;
    private String imageType;
    private String resolution;
    private String classes;
    private String usageExample;
    private List<String> tags;

    @NotNull(message = "파일 ID는 필수입니다.")
    private Long fileId;
}