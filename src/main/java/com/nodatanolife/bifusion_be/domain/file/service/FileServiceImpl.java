package com.nodatanolife.bifusion_be.domain.file.service;

import com.nodatanolife.bifusion_be.domain.file.dto.response.TempFileUploadResponse;
import com.nodatanolife.bifusion_be.domain.file.entity.File;
import com.nodatanolife.bifusion_be.domain.file.repository.FileRepository;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FileServiceImpl implements FileService {

    private final S3Service s3Service;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;

    /**
     * 임시 파일 다건 업로드
     * S3에 파일 업로드 후 File 엔티티 저장
     */
    @Override
    public List<TempFileUploadResponse> uploadTempFiles(Long userId, List<MultipartFile> files) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return files.stream()
                .map(file -> uploadSingleTempFile(user, file))
                .toList();
    }

    /**
     * 회원 탈퇴 시 파일 익명화
     * 유저 소유 파일의 owner를 탈퇴 계정으로 변경
     */
    @Override
    public void anonymizeByUser(User user, User deletedUser) {
        fileRepository.findAllByUser(user)
                .forEach(f -> f.anonymize(deletedUser));
    }

    /**
     * 임시 파일 단건 업로드
     * S3 temp/ 경로에 업로드 후 File 엔티티 생성
     */
    private TempFileUploadResponse uploadSingleTempFile(User user, MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String nameWithoutExt = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(0, originalFilename.lastIndexOf("."))
                : originalFilename;

        String filename = S3Service.buildUniqueFilename(originalFilename);
        String s3Key = "temp/" + filename;
        String url = s3Service.uploadFile(file, "temp", filename);

        File tempFile = File.createTemp(user, url, s3Key, nameWithoutExt, file.getContentType());
        fileRepository.save(tempFile);

        return TempFileUploadResponse.from(tempFile);
    }
}