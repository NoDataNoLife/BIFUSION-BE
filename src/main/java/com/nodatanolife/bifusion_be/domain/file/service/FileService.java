package com.nodatanolife.bifusion_be.domain.file.service;

import com.nodatanolife.bifusion_be.domain.file.dto.response.TempFileUploadResponse;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


public interface FileService {

    List<TempFileUploadResponse> uploadTempFiles(Long userId, List<MultipartFile> files);

    void anonymizeByUser(User user, User deletedUser);
}
