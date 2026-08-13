package com.nodatanolife.bifusion_be.domain.dataset.repository;

import com.nodatanolife.bifusion_be.domain.dataset.entity.Dataset;
import com.nodatanolife.bifusion_be.domain.dataset.entity.DatasetType;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DatasetRepository extends JpaRepository<Dataset, Long> {

    List<Dataset> findAllByUser(User user);

    @EntityGraph(attributePaths = "user")
    Page<Dataset> findAllByTypeAndUser_UserId(DatasetType type, Long userId, Pageable pageable);

    @Query(
            value = "SELECT d FROM Dataset d JOIN FETCH d.user WHERE d.isPublic = true AND (:keyword IS NULL OR d.title LIKE %:keyword%)",
            countQuery = "SELECT COUNT(d) FROM Dataset d WHERE d.isPublic = true AND (:keyword IS NULL OR d.title LIKE %:keyword%)"
    )
    Page<Dataset> findAllByCondition(
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
