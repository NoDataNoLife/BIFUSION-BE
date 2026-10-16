package com.nodatanolife.bifusion_be.domain.job.repository;

import com.nodatanolife.bifusion_be.domain.job.entity.Job;
import com.nodatanolife.bifusion_be.domain.job.entity.JobStatus;
import com.nodatanolife.bifusion_be.domain.job.entity.JobType;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findAllByUser(User user);

    Optional<Job> findByUserUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    /*
     * 프로젝트 Job 목록·상태별 개수. 선택 조건은 "has* = false OR 조건" 플래그 방식으로 처리한다.
     * (PostgreSQL이 ":param IS NULL"만으로는 파라미터 타입을 추론하지 못해 실패하는 것을 피하기 위함)
     * 검색 조건(type·jobId·nickname)은 두 쿼리가 같고, 목록에만 status·커서 조건이 더 붙는다.
     */

    @Query("""
            select j from Job j
            join fetch j.user u
            where j.project.projectId = :projectId
              and (:hasType = false or j.type = :type)
              and (:hasStatus = false or j.status = :status)
              and (:hasJobId = false or j.jobId = :jobId)
              and (:hasNickname = false or lower(u.nickname) like :nicknamePattern escape '\\')
              and (:hasCursor = false or j.createdAt < :cursorCreatedAt
                   or (j.createdAt = :cursorCreatedAt and j.jobId < :cursorJobId))
            order by j.createdAt desc, j.jobId desc
            """)
    List<Job> findProjectJobs(@Param("projectId") Long projectId,
                              @Param("hasType") boolean hasType, @Param("type") JobType type,
                              @Param("hasStatus") boolean hasStatus, @Param("status") JobStatus status,
                              @Param("hasJobId") boolean hasJobId, @Param("jobId") Long jobId,
                              @Param("hasNickname") boolean hasNickname, @Param("nicknamePattern") String nicknamePattern,
                              @Param("hasCursor") boolean hasCursor,
                              @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
                              @Param("cursorJobId") Long cursorJobId,
                              Pageable pageable);

    @Query("""
            select new com.nodatanolife.bifusion_be.domain.job.repository.JobStatusCount(j.status, count(j))
            from Job j
            join j.user u
            where j.project.projectId = :projectId
              and (:hasType = false or j.type = :type)
              and (:hasJobId = false or j.jobId = :jobId)
              and (:hasNickname = false or lower(u.nickname) like :nicknamePattern escape '\\')
            group by j.status
            """)
    List<JobStatusCount> countProjectJobsByStatus(@Param("projectId") Long projectId,
                                                  @Param("hasType") boolean hasType, @Param("type") JobType type,
                                                  @Param("hasJobId") boolean hasJobId, @Param("jobId") Long jobId,
                                                  @Param("hasNickname") boolean hasNickname,
                                                  @Param("nicknamePattern") String nicknamePattern);

    /**
     * 재시도 원본 Job을 행 잠금으로 조회한다. 같은 Job에 재시도가 동시에 들어와도 하나만 통과시키기 위해.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from Job j where j.jobId = :jobId")
    Optional<Job> findByIdForUpdate(@Param("jobId") Long jobId);

    /**
     * 폴링 대상. ML 접수 시각(없으면 생성 시각)이 오래된 순.
     * mlJobId 없는 Job은 startedAt이 null이라, 정렬에서 밀려 처리되지 못하는 일이 없도록 생성 시각으로 대신한다.
     */
    @Query("select j from Job j where j.status in :statuses order by coalesce(j.startedAt, j.createdAt) asc, j.jobId asc")
    List<Job> findPollTargets(@Param("statuses") Collection<JobStatus> statuses, Pageable pageable);
}
