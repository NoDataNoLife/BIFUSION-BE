package com.nodatanolife.bifusion_be.domain.file.dto.response;

import com.nodatanolife.bifusion_be.domain.file.entity.File;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TempFileUploadResponse {

    private Long fileId;
    private String fileUrl;

    public static TempFileUploadResponse from(File file) {
        return TempFileUploadResponse.builder()
                .fileId(file.getFileId())
                .fileUrl(file.getFileUrl())
                .build();
    }
}
