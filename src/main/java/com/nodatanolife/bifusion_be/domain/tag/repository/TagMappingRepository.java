package com.nodatanolife.bifusion_be.domain.tag.repository;

import com.nodatanolife.bifusion_be.domain.tag.TagMapping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagMappingRepository extends JpaRepository<TagMapping, Long> {
}
