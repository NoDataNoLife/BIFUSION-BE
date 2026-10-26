package com.nodatanolife.bifusion_be.domain.dataset.service;

import com.nodatanolife.bifusion_be.domain.file.entity.File;
import com.nodatanolife.bifusion_be.domain.file.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 업로드 데이터셋의 첨부 파일에서 대표 이미지를 뽑는다.
 * - 첨부 파일이 이미지면 그 파일을 그대로 쓴다 (새로 올리지 않는다).
 * - zip이면 안에 든 첫 번째 이미지를 꺼내 dataset-banners/에 올린다.
 * - 그 외 형식이거나 뽑지 못하면 null. 대표 이미지는 부가 정보라 실패해도 데이터셋 등록·수정을 막지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatasetBannerExtractor {

    private static final String BANNER_PREFIX = "dataset-banners/";
    // 압축 해제 시 이미지 한 장의 상한. 업로드 상한(10MB)을 넘는 항목은 비정상 압축으로 보고 건너뛴다.
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final Map<String, String> IMAGE_CONTENT_TYPES = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp",
            "gif", "image/gif",
            "bmp", "image/bmp"
    );

    private final S3Service s3Service;

    /**
     * @return 대표 이미지의 S3 key. 뽑을 수 없으면 null
     */
    public String extract(File file) {
        if (file == null || file.getS3Key() == null) {
            return null;
        }
        String fileType = file.getFileType() != null ? file.getFileType().toLowerCase(Locale.ROOT) : "";
        try {
            if (fileType.startsWith("image/")) {
                return file.getS3Key();
            }
            if (fileType.contains("zip")) {
                return extractFromZip(file);
            }
            return null;
        } catch (Exception e) {
            log.warn("데이터셋 대표 이미지 추출 실패: fileId={}, err={}", file.getFileId(), e.getClass().getSimpleName());
            return null;
        }
    }

    private String extractFromZip(File file) throws IOException {
        byte[] zipBytes = s3Service.downloadFile(file.getS3Key());
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String contentType = imageContentType(entry);
                if (contentType == null) {
                    continue;
                }
                byte[] image = readLimited(zip);
                if (image == null) {
                    continue;
                }
                String name = baseName(entry.getName());
                return s3Service.uploadBytes(image, BANNER_PREFIX + S3Service.buildUniqueFilename(name), contentType);
            }
        }
        return null;
    }

    /**
     * 이미지 항목이면 content type, 아니면 null. 폴더와 macOS 메타데이터(__MACOSX/, ._파일)는 건너뛴다.
     */
    private static String imageContentType(ZipEntry entry) {
        if (entry.isDirectory()) {
            return null;
        }
        String path = entry.getName();
        String name = baseName(path);
        if (path.startsWith("__MACOSX/") || name.startsWith(".")) {
            return null;
        }
        int dot = name.lastIndexOf('.');
        return dot < 0 ? null : IMAGE_CONTENT_TYPES.get(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    private static String baseName(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    // 상한을 넘으면 null
    private static byte[] readLimited(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            total += read;
            if (total > MAX_IMAGE_BYTES) {
                return null;
            }
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }
}
