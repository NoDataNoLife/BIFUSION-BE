package com.nodatanolife.bifusion_be.domain.file.service;

import com.nodatanolife.bifusion_be.domain.file.dto.response.TempFileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


public interface FileService {

    // 임시 파일 다건 업로드 API
    List<TempFileUploadResponse> uploadTempFiles(List<MultipartFile> files);
}
