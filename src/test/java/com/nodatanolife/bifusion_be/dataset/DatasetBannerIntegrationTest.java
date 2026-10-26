package com.nodatanolife.bifusion_be.dataset;

import com.nodatanolife.bifusion_be.domain.dataset.dto.request.DatasetUpdateRequest;
import com.nodatanolife.bifusion_be.domain.dataset.entity.DatasetType;
import com.nodatanolife.bifusion_be.domain.dataset.repository.DatasetRepository;
import com.nodatanolife.bifusion_be.domain.file.entity.File;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import com.nodatanolife.bifusion_be.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 업로드 데이터셋 대표 이미지 추출, 커뮤니티 목록·상세 노출, 기존 데이터셋 채우기.
 */
class DatasetBannerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    DatasetRepository datasetRepository;

    @Test
    void 첨부_파일이_이미지면_그_파일을_대표_이미지로_쓴다() {
        User owner = user("owner@test.com", 0);
        File image = tempFile(owner, "image/png", 1000);

        Long datasetId = dataset(owner, true, image);

        assertThat(datasetService.getDatasetDetail(datasetId, owner.getUserId()).bannerImageUrl()).isEqualTo(image.getS3Key());
        assertThat(datasetService.getDatasets("LATEST", null, 0, 10).getContent().get(0).bannerImageUrl()).isEqualTo(image.getS3Key());
        assertThat(datasetService.getMyDatasets(DatasetType.UPLOADED, owner.getUserId(), 0, 10).getContent().get(0).bannerImageUrl())
                .isEqualTo(image.getS3Key());
        verify(s3Service, never()).uploadBytes(any(), any(), any());
    }

    @Test
    void zip이면_메타데이터를_건너뛰고_첫_번째_이미지를_꺼내_올린다() throws IOException {
        User owner = user("owner@test.com", 0);
        File zip = tempFile(owner, "application/zip", 1000);
        when(s3Service.downloadFile(zip.getS3Key())).thenReturn(zip(
                "__MACOSX/._a.png", "images/", "readme.txt", "images/.hidden.png", "images/b.png", "images/c.jpg"));

        Long datasetId = dataset(owner, true, zip);

        String banner = datasetService.getDatasetDetail(datasetId, owner.getUserId()).bannerImageUrl();
        assertThat(banner).startsWith("dataset-banners/").endsWith("_b.png");
        verify(s3Service).uploadBytes(any(), startsWith("dataset-banners/"), eq("image/png"));
    }

    @Test
    void 이미지가_아닌_파일이거나_추출에_실패해도_등록은_되고_대표_이미지는_비어_있다() {
        User owner = user("owner@test.com", 0);
        Long csvDataset = dataset(owner, true, tempFile(owner, "text/csv", 1000));
        File brokenZip = tempFile(owner, "application/zip", 1000);
        when(s3Service.downloadFile(brokenZip.getS3Key())).thenThrow(new BusinessException(ErrorCode.FILE_NOT_FOUND));
        Long brokenDataset = dataset(owner, true, brokenZip);

        assertThat(datasetService.getDatasetDetail(csvDataset, owner.getUserId()).bannerImageUrl()).isNull();
        assertThat(datasetService.getDatasetDetail(brokenDataset, owner.getUserId()).bannerImageUrl()).isNull();
    }

    @Test
    void 첨부_파일을_바꾸면_대표_이미지도_다시_뽑는다() {
        User owner = user("owner@test.com", 0);
        Long datasetId = dataset(owner, true, tempFile(owner, "text/csv", 1000));
        File image = tempFile(owner, "image/jpeg", 1000);

        datasetService.updateDataset(owner.getUserId(), datasetId, request(DatasetUpdateRequest.class, Map.of(
                "title", "데이터셋", "description", "설명", "fileId", image.getFileId(), "tags", List.of())));

        assertThat(datasetService.getDatasetDetail(datasetId, owner.getUserId()).bannerImageUrl()).isEqualTo(image.getS3Key());
    }

    @Test
    void 대표_이미지가_비어_있는_기존_데이터셋을_채운다() {
        User owner = user("owner@test.com", 0);
        File image = tempFile(owner, "image/png", 1000);
        Long datasetId = dataset(owner, true, image);
        Long csvDataset = dataset(owner, true, tempFile(owner, "text/csv", 1000));
        jdbc.update("UPDATE dataset SET banner_image_url = NULL");

        assertThat(datasetRepository.findIdsWithoutBannerImage()).containsExactlyInAnyOrder(datasetId, csvDataset);
        assertThat(datasetService.backfillBannerImage(datasetId)).isTrue();
        assertThat(datasetService.backfillBannerImage(csvDataset)).isFalse();

        assertThat(datasetService.getDatasetDetail(datasetId, owner.getUserId()).bannerImageUrl()).isEqualTo(image.getS3Key());
        assertThat(datasetRepository.findIdsWithoutBannerImage()).containsExactly(csvDataset);
    }

    private static byte[] zip(String... entries) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (String entry : entries) {
                zip.putNextEntry(new ZipEntry(entry));
                if (!entry.endsWith("/")) {
                    zip.write(entry.getBytes());
                }
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }
}
