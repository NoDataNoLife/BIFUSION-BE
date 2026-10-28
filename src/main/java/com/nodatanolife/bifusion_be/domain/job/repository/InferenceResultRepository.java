package com.nodatanolife.bifusion_be.domain.job.repository;

import com.nodatanolife.bifusion_be.domain.job.entity.InferenceResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InferenceResultRepository extends JpaRepository<InferenceResult, Long> {

    List<InferenceResult> findAllByInferenceConfigJobJobIdOrderBySampleIndexAsc(Long jobId);

    /**
     * 프로젝트 삭제 시 추론 결과의 Top-3 support를 지운다. 추론 결과(deleteAllByProjectId)보다 먼저 지워야 한다.
     */
    @Modifying
    @Query("""
            delete from Top3Support s
            where s.inferenceResult.inferenceResultId in (
                select r.inferenceResultId from InferenceResult r where r.inferenceConfig.job.project.projectId = :projectId)
            """)
    int deleteTop3SupportsByProjectId(@Param("projectId") Long projectId);

    // 프로젝트 삭제 시 추론 결과를 지운다 (결과 파일 row를 지우기 전에 FK를 끊는다)
    @Modifying
    @Query("""
            delete from InferenceResult r
            where r.inferenceConfig.inferenceConfigId in (
                select c.inferenceConfigId from InferenceConfig c where c.job.project.projectId = :projectId)
            """)
    int deleteAllByProjectId(@Param("projectId") Long projectId);
}
