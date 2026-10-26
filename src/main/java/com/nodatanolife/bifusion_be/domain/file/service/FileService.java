package com.nodatanolife.bifusion_be.domain.file.service;

import com.nodatanolife.bifusion_be.domain.file.dto.response.FileDownloadResponse;
import com.nodatanolife.bifusion_be.domain.file.dto.response.TempFileUploadResponse;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


public interface FileService {

    List<TempFileUploadResponse> uploadTempFiles(Long userId, List<MultipartFile> files);

    void anonymizeByUser(User user, User deletedUser);

    /**
     * fileId로 파일을 조회해 presigned 다운로드 URL을 발급한다.
     * 데이터셋 등 파일을 참조하는 모든 도메인이 공통으로 사용하는 다운로드 API.
     */
    /**
     * 받을 권한이 없는 파일은 존재 여부를 드러내지 않도록 없는 파일과 같은 404(FILE_NOT_FOUND)로 처리한다.
     */
    FileDownloadResponse getDownloadUrl(Long userId, Long fileId);
}
