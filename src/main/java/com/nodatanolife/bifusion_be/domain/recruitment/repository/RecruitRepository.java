package com.nodatanolife.bifusion_be.domain.recruitment.repository;

import com.nodatanolife.bifusion_be.domain.recruitment.entity.Recruit;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RecruitRepository extends JpaRepository<Recruit, Long> {

    List<Recruit> findAllByUser(User user);

    @Query("SELECT r FROM Recruit r WHERE r.isPublic = true AND r.deletedAt IS NULL")
    Page<Recruit> findAllByIsPublicTrue(Pageable pageable);

}
