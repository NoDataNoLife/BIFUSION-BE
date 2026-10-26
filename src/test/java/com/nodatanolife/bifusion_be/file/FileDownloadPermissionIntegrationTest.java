package com.nodatanolife.bifusion_be.file;

import com.nodatanolife.bifusion_be.domain.expert.dto.request.InspectionCreateRequest;
import com.nodatanolife.bifusion_be.domain.expert.entity.InspectionTargetType;
import com.nodatanolife.bifusion_be.domain.expert.service.InspectionService;
import com.nodatanolife.bifusion_be.domain.file.entity.File;
import com.nodatanolife.bifusion_be.domain.file.service.FileService;
import com.nodatanolife.bifusion_be.domain.job.entity.Job;
import com.nodatanolife.bifusion_be.domain.job.entity.JobType;
import com.nodatanolife.bifusion_be.domain.job.repository.JobRepository;
import com.nodatanolife.bifusion_be.domain.project.entity.ProjectRole;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.global.exception.ErrorCode;
import com.nodatanolife.bifusion_be.global.exception.business.BusinessException;
import com.nodatanolife.bifusion_be.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 파일 다운로드 URL 발급 시 파일 종류별 접근 권한. 권한이 없으면 없는 파일과 같은 404.
 */
class FileDownloadPermissionIntegrationTest extends IntegrationTestSupport {

    @Autowired
    FileService fileService;
    @Autowired
    InspectionService inspectionService;
    @Autowired
    JobRepository jobRepository;

    @Test
    void 공개_데이터셋_파일은_누구나_받을_수_있다() {
        User owner = user("owner@test.com", 0);
        User other = user("other@test.com", 0);
        File file = tempFile(owner, "application/zip", 1000);
        dataset(owner, true, file);

        assertThat(fileService.getDownloadUrl(other.getUserId(), file.getFileId()).presignedUrl()).isEqualTo(SIGNED_URL);
    }

    @Test
    void 비공개_데이터셋_파일은_작성자와_검수를_배정받은_전문가만_받을_수_있다() {
        User owner = user("owner@test.com", 1000);
        User other = user("other@test.com", 0);
        User expert = expert("expert@test.com", 0);
        File file = tempFile(owner, "application/zip", 1000);
        Long datasetId = dataset(owner, false, file);

        assertNotFound(other, file);
        assertNotFound(expert, file);
        assertThat(fileService.getDownloadUrl(owner.getUserId(), file.getFileId()).presignedUrl()).isEqualTo(SIGNED_URL);

        inspectionService.createInspection(owner, request(InspectionCreateRequest.class, Map.of(
                "targetType", InspectionTargetType.UPLOADED_DATA, "targetId", datasetId, "reason", "검수", "rewardPoints", 500)));

        assertThat(fileService.getDownloadUrl(expert.getUserId(), file.getFileId()).presignedUrl()).isEqualTo(SIGNED_URL);
        assertNotFound(other, file);
    }

    @Test
    void 삭제된_데이터셋_파일은_작성자만_받을_수_있다() {
        User owner = user("owner@test.com", 0);
        User other = user("other@test.com", 0);
        File file = tempFile(owner, "application/zip", 1000);
        Long datasetId = dataset(owner, true, file);

        datasetService.deleteDataset(owner.getUserId(), datasetId);

        assertNotFound(other, file);
        assertThat(fileService.getDownloadUrl(owner.getUserId(), file.getFileId()).presignedUrl()).isEqualTo(SIGNED_URL);
    }

    @Test
    void 임시_업로드_파일은_올린_본인만_받을_수_있다() {
        User owner = user("owner@test.com", 0);
        User other = user("other@test.com", 0);
        File file = tempFile(owner, "image/png", 1000);

        assertNotFound(other, file);
        assertThat(fileService.getDownloadUrl(owner.getUserId(), file.getFileId()).presignedUrl()).isEqualTo(SIGNED_URL);
    }

    @Test
    void AI_작업_파일은_그_프로젝트_멤버만_받을_수_있다() {
        User owner = user("owner@test.com", 0);
        User member = user("member@test.com", 0);
        User outsider = user("outsider@test.com", 0);
        Long projectId = project(owner);
        addMember(projectId, member, ProjectRole.MEMBER);
        Job job = jobRepository.save(Job.create(projectRepository.findById(projectId).orElseThrow(), owner,
                JobType.AUGMENTATION, 1L, "idem-file"));
        File result = fileRepository.save(File.createJobResult(owner, job.getJobId(), "ml-results/out.zip", "application/zip", 1000L));

        assertThat(fileService.getDownloadUrl(member.getUserId(), result.getFileId()).presignedUrl()).isEqualTo(SIGNED_URL);
        assertNotFound(outsider, result);
    }

    @Test
    void 없는_파일은_404다() {
        User user = user("user@test.com", 0);

        assertThatThrownBy(() -> fileService.getDownloadUrl(user.getUserId(), 999_999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FILE_NOT_FOUND);
    }

    private void assertNotFound(User user, File file) {
        assertThatThrownBy(() -> fileService.getDownloadUrl(user.getUserId(), file.getFileId()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FILE_NOT_FOUND);
    }
}
