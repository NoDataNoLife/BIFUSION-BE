package com.nodatanolife.bifusion_be.domain.file;

import com.nodatanolife.bifusion_be.domain.file.entity.File;
import com.nodatanolife.bifusion_be.domain.file.repository.FileRepository;
import com.nodatanolife.bifusion_be.domain.file.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TempFileCleanupScheduler {

    private final FileRepository fileRepository;
    private final S3Service s3Service;

    @Scheduled(fixedRate = 30 * 60 * 1000)
    @Transactional
    public void cleanUpExpiredTempFiles() {
        LocalDateTime expiryTime = LocalDateTime.now().minusHours(1);
        List<File> expiredFiles = fileRepository.findAllByIsTempTrueAndCreatedAtBefore(expiryTime);

        log.info("[TempFileCleanup] 만료된 임시 파일 {}건 삭제 시작", expiredFiles.size());

        int successCount = 0;
        int failCount = 0;

        for (File file : expiredFiles) {
            try {
                s3Service.deleteFile(file.getS3Key());
                fileRepository.delete(file);
                successCount++;
            } catch (Exception e) {
                log.warn("[TempFileCleanup] 파일 삭제 실패 - fileId: {}, url: {}, 사유: {}",
                        file.getFileId(), file.getFileUrl(), e.getMessage());
                failCount++;
            }
        }

        log.info("[TempFileCleanup] 삭제 완료 - 성공: {}건, 실패: {}건", successCount, failCount);
    }
}
