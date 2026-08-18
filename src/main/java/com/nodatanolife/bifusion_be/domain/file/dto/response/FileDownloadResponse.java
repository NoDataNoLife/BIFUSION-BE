package com.nodatanolife.bifusion_be.domain.file.dto.response;

import com.nodatanolife.bifusion_be.domain.file.entity.File;

public record FileDownloadResponse(
        Long fileId,
        String fileName,
        String presignedUrl,
        Integer expiresIn
) {
    public static FileDownloadResponse from(File file, String presignedUrl, int expiresIn) {
        return new FileDownloadResponse(
                file.getFileId(),
                file.getFileName(),
                presignedUrl,
                expiresIn
        );
    }
}
