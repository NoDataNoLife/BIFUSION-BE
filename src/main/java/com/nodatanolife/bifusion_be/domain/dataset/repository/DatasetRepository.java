package com.nodatanolife.bifusion_be.domain.dataset.repository;

import com.nodatanolife.bifusion_be.domain.dataset.entity.Dataset;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DatasetRepository extends JpaRepository<Dataset, Long> {

    List<Dataset> findAllByUser(User user);
}
