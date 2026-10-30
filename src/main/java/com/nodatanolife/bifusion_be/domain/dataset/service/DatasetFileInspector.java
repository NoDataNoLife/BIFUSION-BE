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
 * 업로드 데이터셋의 첨부 파일에서 대표 이미지와 파일 수를 뽑는다. zip은 한 번만 내려받아 둘을 함께 구한다.
 * - 대표 이미지: 첨부 파일이 이미지면 그 파일을 그대로 쓰고(새로 올리지 않는다), zip이면 안에 든 첫 번째 이미지를 꺼내 dataset-banners/에 올린다.
 * - 파일 수: zip이면 안에 든 파일 수(폴더·macOS 메타데이터 제외), 그 외 형식은 1.
 * - 뽑지 못한 값은 null. 둘 다 부가 정보라 실패해도 데이터셋 등록·수정을 막지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatasetFileInspector {

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

    public DatasetFileSummary inspect(File file) {
        return inspect(file, true);
    }

    /**
     * 파일 수만 센다. 대표 이미지를 올리지 않는다 (이미 등록된 데이터셋의 파일 수 채우기용).
     */
    public Integer countFiles(File file) {
        return inspect(file, false).fileCount();
    }

    private DatasetFileSummary inspect(File file, boolean withBanner) {
        if (file == null || file.getS3Key() == null) {
            return new DatasetFileSummary(null, null);
        }
        String fileType = file.getFileType() != null ? file.getFileType().toLowerCase(Locale.ROOT) : "";
        if (!fileType.contains("zip")) {
            return new DatasetFileSummary(withBanner && fileType.startsWith("image/") ? file.getS3Key() : null, 1);
        }
        try {
            return inspectZip(file, withBanner);
        } catch (Exception e) {
            log.warn("데이터셋 첨부 파일 분석 실패: fileId={}, err={}", file.getFileId(), e.getClass().getSimpleName());
            return new DatasetFileSummary(null, null);
        }
    }

    /**
     * 전체 항목을 끝까지 센 뒤에 대표 이미지를 올린다. 중간에 실패하면 올린 이미지가 버려지지 않도록 하기 위해서다.
     */
    private DatasetFileSummary inspectZip(File file, boolean withBanner) throws IOException {
        byte[] zipBytes = s3Service.downloadFile(file.getS3Key());
        int fileCount = 0;
        byte[] bannerImage = null;
        String bannerName = null;
        String bannerContentType = null;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!isDataFile(entry)) {
                    continue;
                }
                fileCount++;
                String contentType = imageContentType(entry);
                if (!withBanner || bannerImage != null || contentType == null) {
                    continue;
                }
                bannerImage = readLimited(zip);
                bannerName = baseName(entry.getName());
                bannerContentType = contentType;
            }
        }
        String bannerKey = bannerImage != null
                ? s3Service.uploadBytes(bannerImage, BANNER_PREFIX + S3Service.buildUniqueFilename(bannerName), bannerContentType)
                : null;
        return new DatasetFileSummary(bannerKey, fileCount);
    }

    /**
     * 데이터 파일이면 true. 폴더와 macOS 메타데이터(__MACOSX/, ._파일 등 .으로 시작하는 파일)는 건너뛴다.
     */
    private static boolean isDataFile(ZipEntry entry) {
        if (entry.isDirectory()) {
            return false;
        }
        String path = entry.getName();
        return !path.startsWith("__MACOSX/") && !baseName(path).startsWith(".");
    }

    /**
     * 이미지 항목이면 content type, 아니면 null. 데이터 파일인지는 isDataFile로 먼저 거른다.
     */
    private static String imageContentType(ZipEntry entry) {
        String name = baseName(entry.getName());
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
