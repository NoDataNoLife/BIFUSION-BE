package com.nodatanolife.bifusion_be.domain.job.repository;

import com.nodatanolife.bifusion_be.domain.job.entity.Job;
import com.nodatanolife.bifusion_be.domain.job.entity.JobStatus;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findAllByUser(User user);

    Optional<Job> findByUserUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    /**
     * 폴링 대상. ML 접수 시각(없으면 생성 시각)이 오래된 순.
     * mlJobId 없는 Job은 startedAt이 null이라, 정렬에서 밀려 처리되지 못하는 일이 없도록 생성 시각으로 대신한다.
     */
    @Query("select j from Job j where j.status in :statuses order by coalesce(j.startedAt, j.createdAt) asc, j.jobId asc")
    List<Job> findPollTargets(@Param("statuses") Collection<JobStatus> statuses, Pageable pageable);
}
