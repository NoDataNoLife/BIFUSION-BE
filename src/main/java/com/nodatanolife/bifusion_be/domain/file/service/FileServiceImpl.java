package com.nodatanolife.bifusion_be.domain.file.service;

import com.nodatanolife.bifusion_be.domain.file.DTO.response.TempFileUploadResponse;
import com.nodatanolife.bifusion_be.domain.file.entity.File;
import com.nodatanolife.bifusion_be.domain.file.repository.FileRepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FileServiceImpl implements FileService{

    private final S3Service s3Service;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;

    @Override
    public List<TempFileUploadResponse> uploadTempFiles(List<MultipartFile> files) {
        return files.stream()
                .map(this::uploadSingleTempFile)
                .toList();
    }

    // 임시 파일 단건 업로드
    private TempFileUploadResponse uploadSingleTempFile(MultipartFile file) {
        // TODO: 로그인 구현 후 수정예정
        User user = userRepository.findById(1L).orElse(null);

        String originalFilename = file.getOriginalFilename();
        String nameWithoutExt = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(0, originalFilename.lastIndexOf("."))
                : originalFilename;

        String filename = UUID.randomUUID() + "_" + originalFilename;
        String url = s3Service.uploadFile(file, "temp", filename);

        File tempFile = File.createTemp(user, url, nameWithoutExt, file.getContentType());
        fileRepository.save(tempFile);

        return TempFileUploadResponse.from(tempFile);
    }
}