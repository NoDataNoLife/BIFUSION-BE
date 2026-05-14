package com.nodatanolife.bifusion_be.domain.job.repository;

import com.nodatanolife.bifusion_be.domain.job.entity.JobComment;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobCommentRepository extends JpaRepository<JobComment, Long> {

    List<JobComment> findByJobJobId(Long jobId);

    List<JobComment> findAllByUser(User user);
}