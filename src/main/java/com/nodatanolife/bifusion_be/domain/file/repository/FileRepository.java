package com.nodatanolife.bifusion_be.domain.file.repository;

import com.nodatanolife.bifusion_be.domain.file.entity.File;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface FileRepository extends JpaRepository<File, Long> {

    List<File> findAllByIsTempTrueAndCreatedAtBefore(LocalDateTime expiryTime);

    /**
     * Job 입력 파일 검증·연결용. 같은 임시 파일로 동시에 두 Job이 만들어지지 않도록 행 잠금을 건다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from File f where f.fileId in :fileIds")
    List<File> findAllByIdForUpdate(@Param("fileIds") Collection<Long> fileIds);

    List<File> findAllByUser(User user);

    // Job 입력·결과 파일 (targetType = JOB / JOB_RESULT)
    List<File> findAllByTargetTypeAndTargetId(String targetType, Long targetId);

    /**
     * 프로젝트 삭제 시 정리할 Job 입력·결과 파일의 S3 key. 데이터셋에 첨부된 파일은 데이터셋이 계속 쓰므로 뺀다.
     * deleteJobFilesByProjectId와 같은 조건이다.
     */
    @Query("""
            select f.s3Key from File f
            where f.targetType in ('JOB', 'JOB_RESULT')
              and f.targetId in (select j.jobId from Job j where j.project.projectId = :projectId)
              and not exists (select 1 from Dataset d where d.file = f)
            """)
    List<String> findJobFileKeysByProjectId(@Param("projectId") Long projectId);

    /**
     * 프로젝트 삭제 시 Job 입력·결과 파일 row를 한 번에 지운다 (저장 용량 집계에서 빠진다).
     * 이 파일을 참조하는 추론 결과(InferenceResult·Top3Support)는 먼저 지워야 한다.
     */
    @Modifying
    @Query("""
            delete from File f
            where f.targetType in ('JOB', 'JOB_RESULT')
              and f.targetId in (select j.jobId from Job j where j.project.projectId = :projectId)
              and not exists (select 1 from Dataset d where d.file = f)
            """)
    int deleteJobFilesByProjectId(@Param("projectId") Long projectId);

    /**
     * 주어진 key 중 데이터셋에 첨부된 파일의 key. 데이터셋이 계속 참조하므로 row도 S3 객체도 지우면 안 된다.
     * (데이터셋 생성 시 소유자만 확인해, 본인의 Job 결과 파일도 데이터셋에 첨부할 수 있다)
     */
    @Query("select f.s3Key from File f where f.s3Key in :s3Keys and exists (select 1 from Dataset d where d.file = f)")
    List<String> findDatasetAttachedKeys(@Param("s3Keys") Collection<String> s3Keys);

    // 증강 결과 파일 크기 조회용 (AugmentationConfig.datasetKey → File)
    List<File> findAllByS3KeyIn(Collection<String> s3Keys);

    /**
     * 사용자의 저장 용량 집계. 소유자 기준이며, 1시간 뒤 정리되는 임시 업로드와 삭제된 데이터셋의 파일은 제외한다.
     * 데이터셋 첨부 파일은 그 파일을 쓰는 데이터셋이 모두 삭제됐을 때만 빠진다 (삭제 시 S3 객체도 지운다).
     * 크기 정보가 없는 파일(결과 파일 크기 조회 실패 등)은 합계에서 빠지고 unmeasuredCount로 따로 센다.
     */
    @Query("""
            select coalesce(sum(f.fileSize), 0) as usedBytes,
                   coalesce(sum(case when f.fileSize is null then 1 else 0 end), 0) as unmeasuredCount
            from File f
            where f.user.userId = :userId
              and coalesce(f.isTemp, false) = false
              and not (exists (select 1 from Dataset d where d.file = f)
                       and not exists (select 1 from Dataset d where d.file = f and d.deletedAt is null))
            """)
    StorageUsage sumStorageUsage(@Param("userId") Long userId);

    interface StorageUsage {
        Long getUsedBytes();
        Long getUnmeasuredCount();
    }
}