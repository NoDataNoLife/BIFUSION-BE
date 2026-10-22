package com.nodatanolife.bifusion_be.domain.file.repository;

import com.nodatanolife.bifusion_be.domain.file.entity.File;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    /**
     * 사용자의 저장 용량 집계. 소유자 기준이며, 1시간 뒤 정리되는 임시 업로드와 삭제된 데이터셋의 파일은 제외한다.
     * 크기 정보가 없는 파일(결과 파일 크기 조회 실패 등)은 합계에서 빠지고 unmeasuredCount로 따로 센다.
     */
    @Query("""
            select coalesce(sum(f.fileSize), 0) as usedBytes,
                   coalesce(sum(case when f.fileSize is null then 1 else 0 end), 0) as unmeasuredCount
            from File f
            where f.user.userId = :userId
              and coalesce(f.isTemp, false) = false
              and not exists (select 1 from Dataset d where d.file = f and d.deletedAt is not null)
            """)
    StorageUsage sumStorageUsage(@Param("userId") Long userId);

    interface StorageUsage {
        Long getUsedBytes();
        Long getUnmeasuredCount();
    }
}