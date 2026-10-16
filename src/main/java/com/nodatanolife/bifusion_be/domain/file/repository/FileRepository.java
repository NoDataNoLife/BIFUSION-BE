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

    /**
     * 임시 보관 시작 시각(temp_since, 없으면 created_at)이 기준 시각 이전인 임시 파일.
     * temp_since가 없는 기존 파일은 지금처럼 created_at 기준으로 정리된다.
     */
    @Query("select f from File f where f.isTemp = true and coalesce(f.tempSince, f.createdAt) < :expiryTime")
    List<File> findExpiredTempFiles(@Param("expiryTime") LocalDateTime expiryTime);

    List<File> findAllByTargetTypeAndTargetId(String targetType, Long targetId);

    /**
     * Job 입력 파일 검증·연결용. 같은 임시 파일로 동시에 두 Job이 만들어지지 않도록 행 잠금을 건다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from File f where f.fileId in :fileIds")
    List<File> findAllByIdForUpdate(@Param("fileIds") Collection<Long> fileIds);

    List<File> findAllByUser(User user);
}