package com.nodatanolife.bifusion_be.domain.file.repository;

import com.nodatanolife.bifusion_be.domain.file.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FileRepository extends JpaRepository<File, Long> {
}