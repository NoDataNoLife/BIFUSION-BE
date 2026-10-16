package com.nodatanolife.bifusion_be.domain.file.service;

import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private static final String S3_HOST_MARKER = ".amazonaws.com/";

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${spring.cloud.aws.region.static}")
    private String region;

    @Value("${spring.cloud.aws.s3.presign-duration-minutes:10}")
    private long presignDurationMinutes;

    /**
     * 파일명 충돌을 막는 UUID prefix를 붙여 고유 파일명을 만든다.
     * temp/permanent 업로드 양쪽에서 공용.
     */
    public static String buildUniqueFilename(String originalFilename) {
        return UUID.randomUUID() + "_" + originalFilename;
    }

    /**
     * S3 공개 URL에서 객체 key를 추출한다. URL 형식이 예상과 다르면 null.
     */
    public static String extractKeyFromUrl(String url) {
        if (url == null) return null;
        int idx = url.indexOf(S3_HOST_MARKER);
        return idx < 0 ? null : url.substring(idx + S3_HOST_MARKER.length());
    }

    /**
     * DB에 저장된 값(S3 공개 URL 또는 raw key)을 만료 시간이 있는 presigned GET URL로 변환한다.
     * 버킷은 private 상태이므로 응답 시점에 이 메서드로 접근 가능한 URL을 발급한다.
     * 입력이 비어있거나 key를 추출할 수 없으면 원본을 그대로 반환한다(외부 URL 등).
     */
    public String toPresignedUrl(String storedUrlOrKey) {
        return toPresignedUrl(storedUrlOrKey, Duration.ofMinutes(presignDurationMinutes));
    }

    /**
     * 만료 시간을 호출 측에서 직접 지정해 presigned GET URL을 발급한다.
     * (예: 다운로드 API처럼 응답의 expiresIn 값과 실제 URL 만료 시간이 정확히 일치해야 하는 경우)
     */
    public String toPresignedUrl(String storedUrlOrKey, Duration duration) {
        if (storedUrlOrKey == null || storedUrlOrKey.isBlank()) {
            return storedUrlOrKey;
        }

        String key;
        if (storedUrlOrKey.contains(S3_HOST_MARKER)) {
            // 우리 S3 공개 URL → key 추출 후 presign
            key = extractKeyFromUrl(storedUrlOrKey);
        } else if (storedUrlOrKey.startsWith("http://") || storedUrlOrKey.startsWith("https://")) {
            // S3가 아닌 외부 URL(예: OAuth 제공자 프로필 이미지) → 그대로 노출
            return storedUrlOrKey;
        } else {
            // scheme 없는 값 → raw S3 key로 간주
            key = storedUrlOrKey;
        }
        if (key == null || key.isBlank()) {
            return storedUrlOrKey;
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(duration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            log.warn("Presigned URL 생성 실패, 원본 반환: key={}, err={}", key, e.getMessage());
            return storedUrlOrKey;
        }
    }

    /**
     * 파일명까지 호출 측에서 지정해 업로드한다 (temp 업로드 등 s3Key를 함께 보관해야 하는 케이스).
     */
    public String uploadFile(MultipartFile file, String prefix, String filename) {
        String s3Key = prefix.replaceAll("/+$", "") + "/" + filename;

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(s3Key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
            return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + s3Key;
        } catch (Exception e) {
            log.error("S3 업로드 실패: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * UUID 파일명 자동 생성 후 업로드한다 (호출 측에서 s3Key가 필요 없는 케이스).
     */
    public String uploadFile(MultipartFile file, String prefix) {
        return uploadFile(file, prefix, buildUniqueFilename(file.getOriginalFilename()));
    }

    public void deleteFile(String s3Key) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .build()
        );
    }

    /**
     * 업로드 시 받은 URL로 S3 객체를 삭제한다.실패는 로깅만 한다.
     */
    public void deleteFileByUrl(String url) {
        String key = extractKeyFromUrl(url);
        if (key == null) {
            log.warn("S3 URL 파싱 실패, 삭제 건너뜀: {}", url);
            return;
        }
        try {
            deleteFile(key);
        } catch (Exception e) {
            log.warn("S3 파일 삭제 실패: key={}, err={}", key, e.getMessage());
        }
    }

    public byte[] downloadFile(String prefix, String filename) {
        return downloadFile(prefix.replaceAll("/+$", "") + "/" + filename);
    }

    /**
     * S3 key로 객체를 읽는다. 객체가 없으면(임시 파일 정리 등) FILE_NOT_FOUND.
     */
    public byte[] downloadFile(String s3Key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            ResponseBytes<?> responseBytes = s3Client.getObjectAsBytes(getObjectRequest);
            return responseBytes.asByteArray();
        } catch (NoSuchKeyException e) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        } catch (SdkException e) {
            throw new BusinessException(ErrorCode.FILE_DOWNLOAD_FAILED);
        }
    }
}