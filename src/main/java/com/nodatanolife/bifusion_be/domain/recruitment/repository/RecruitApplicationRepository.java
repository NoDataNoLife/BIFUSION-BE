package com.nodatanolife.bifusion_be.domain.recruitment.repository;

import com.nodatanolife.bifusion_be.domain.recruitment.entity.RecruitApplicationStatus;
import com.nodatanolife.bifusion_be.domain.recruitment.entity.RecruitApplication;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RecruitApplicationRepository extends JpaRepository<RecruitApplication, Long> {

    List<RecruitApplication> findAllByUser(User user);

    boolean existsByRecruit_RecruitIdAndUser_UserId(Long recruitId, Long userId);

    long countByRecruit_RecruitIdAndStatus(Long recruitId, RecruitApplicationStatus status);

    @Query("SELECT ra.recruit.recruitId AS recruitId, COUNT(ra) AS count " +
            "FROM RecruitApplication ra " +
            "WHERE ra.recruit.recruitId IN :recruitIds AND ra.status = :status " +
            "GROUP BY ra.recruit.recruitId")
    List<RecruitMemberCount> countByRecruitIdsAndStatus(
            @Param("recruitIds") List<Long> recruitIds,
            @Param("status") RecruitApplicationStatus status
    );

    interface RecruitMemberCount {
        Long getRecruitId();
        Long getCount();
    }
}
