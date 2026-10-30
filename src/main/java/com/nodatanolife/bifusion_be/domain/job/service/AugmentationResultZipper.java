package com.nodatanolife.bifusion_be.domain.job.service;

import com.nodatanolife.bifusion_be.domain.file.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * 증강 결과 다운로드용 zip(결과 npz + PNG 이미지)을 만들어 S3에 올린다. 트랜잭션을 갖지 않으며 트랜잭션 밖에서 호출한다.
 * npz 다운로드 → PNG 변환·zip 생성(AugmentationPngExporter) → S3 업로드. 임시 파일은 끝나면 지운다.
 * zip은 부가 결과라 실패해도 예외를 던지지 않고 null을 돌려준다. 이때 다운로드는 npz로 남는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AugmentationResultZipper {

    static final String ZIP_CONTENT_TYPE = "application/zip";
    private static final String ZIP_KEY_PREFIX = "augmentation-exports/";

    private final AugmentationPngExporter augmentationPngExporter;
    private final S3Service s3Service;

    /**
     * @param jobCode    화면 표시용 작업 코드 (zip 파일 이름에 쓴다)
     * @param datasetKey 결과 npz의 S3 key
     * @return 올린 zip 정보. 만들지 못하면 null
     */
    public ResultFileMeta create(Long jobId, String jobCode, String datasetKey) {
        // Job마다 key가 고정이라 같은 결과를 다시 만들어도 같은 객체를 덮어쓴다
        String key = ZIP_KEY_PREFIX + jobId + "/" + jobCode + "_augmentation.zip";
        Path dir = null;
        try {
            dir = Files.createTempDirectory("augmentation-zip-");
            Path npz = dir.resolve(AugmentationPngExporter.NPZ_ENTRY_NAME);
            Path zip = dir.resolve("result.zip");
            Files.write(npz, s3Service.downloadFile(datasetKey));
            augmentationPngExporter.export(npz, zip);
            long size = Files.size(zip);
            s3Service.uploadFile(zip, key, ZIP_CONTENT_TYPE);
            return new ResultFileMeta(key, size, ZIP_CONTENT_TYPE);
        } catch (Exception e) {
            log.error("[ResultZip] 증강 결과 zip 생성 실패, npz로 다운로드 - jobId: {}, err: {}", jobId, e.getMessage());
            return null;
        } finally {
            deleteQuietly(dir);
        }
    }

    private static void deleteQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException e) {
            log.warn("[ResultZip] 임시 파일 삭제 실패 - dir: {}", dir);
        }
    }
}
